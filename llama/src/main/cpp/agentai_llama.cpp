// JNI bridge to llama.cpp for Agent AI.
//
// Compiled from llama.cpp source (tag b10453) so it can target Android 7.0 (API 24).
// Exposes the minimum surface the app needs: load a GGUF, feed a chat-formatted prompt,
// and pull generated tokens one at a time.

#include <jni.h>
#include <android/log.h>

#include <algorithm>
#include <string>
#include <vector>
#include <mutex>

#include "llama.h"

#define LOG_TAG "AgentAiLlama"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

struct GenerationState {
    llama_model   * model   = nullptr;
    llama_context * ctx     = nullptr;
    llama_sampler * sampler = nullptr;

    // Absolute position in the KV cache; must keep increasing across turns.
    llama_pos n_past = 0;

    // Tokens currently held in the KV cache. Lets us reuse the shared prefix
    // (system prompt + earlier turns) instead of re-decoding it every message.
    std::vector<llama_token> cached;

    // Remaining tokens allowed for the current reply.
    int  budget      = 0;
    bool generating  = false;

    // Partial UTF-8 sequence carried between tokens so we never emit a broken
    // multi-byte character (critical for Khmer, which is 3 bytes per codepoint).
    std::string utf8_carry;
};

GenerationState g_state;
std::mutex      g_mutex;

bool g_backend_ready = false;

// Returns how many bytes at the tail of `s` form an incomplete UTF-8 sequence.
size_t incomplete_utf8_tail(const std::string & s) {
    const size_t n = s.size();
    for (size_t back = 1; back <= 4 && back <= n; ++back) {
        const auto c = static_cast<unsigned char>(s[n - back]);
        if ((c & 0xC0) == 0x80) {
            continue; // continuation byte, keep walking backwards
        }
        size_t expected = 0;
        if      ((c & 0x80) == 0x00) expected = 1;
        else if ((c & 0xE0) == 0xC0) expected = 2;
        else if ((c & 0xF0) == 0xE0) expected = 3;
        else if ((c & 0xF8) == 0xF0) expected = 4;
        else return 0; // invalid lead byte; let it through rather than stall

        return (back < expected) ? back : 0;
    }
    return 0;
}

void free_context_locked() {
    if (g_state.sampler) { llama_sampler_free(g_state.sampler); g_state.sampler = nullptr; }
    if (g_state.ctx)     { llama_free(g_state.ctx);             g_state.ctx     = nullptr; }
    if (g_state.model)   { llama_model_free(g_state.model);     g_state.model   = nullptr; }
    g_state.n_past     = 0;
    g_state.budget     = 0;
    g_state.generating = false;
    g_state.utf8_carry.clear();
    g_state.cached.clear();
}

} // namespace

extern "C" {

JNIEXPORT jint JNICALL
Java_com_example_llama_LlamaBridge_nativeInit(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_backend_ready) {
        llama_backend_init();
        // llama.cpp is very chatty at info level; keep logcat usable.
        llama_log_set([](ggml_log_level level, const char * text, void *) {
            if (level >= GGML_LOG_LEVEL_ERROR) LOGE("%s", text);
        }, nullptr);
        g_backend_ready = true;
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_example_llama_LlamaBridge_nativeLoadModel(
        JNIEnv * env, jobject, jstring path, jint n_ctx, jint n_threads) {

    std::lock_guard<std::mutex> lock(g_mutex);
    free_context_locked();

    const char * cpath = env->GetStringUTFChars(path, nullptr);
    if (!cpath) return -1;

    auto mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0; // CPU only: Vulkan/OpenCL are unreliable across old devices
    // mmap keeps resident memory low, which matters on 2-3 GB Android 7 devices.
    mparams.load_mode    = LLAMA_LOAD_MODE_MMAP;

    g_state.model = llama_model_load_from_file(cpath, mparams);
    env->ReleaseStringUTFChars(path, cpath);

    if (!g_state.model) {
        LOGE("failed to load model");
        return -2;
    }

    auto cparams = llama_context_default_params();
    cparams.n_ctx       = static_cast<uint32_t>(n_ctx);
    cparams.n_batch     = 256;
    cparams.n_ubatch    = 256; // keep the physical batch aligned with the logical one
    cparams.n_threads   = n_threads;
    cparams.n_threads_batch = n_threads;
    cparams.no_perf     = true;

    g_state.ctx = llama_init_from_model(g_state.model, cparams);
    if (!g_state.ctx) {
        LOGE("failed to create context");
        free_context_locked();
        return -3;
    }

    auto sparams = llama_sampler_chain_default_params();
    sparams.no_perf = true;
    g_state.sampler = llama_sampler_chain_init(sparams);
    // top_k first shrinks the candidate set before the costlier steps run.
    // 30 is plenty for a 1B vocab and slightly cheaper than 40.
    llama_sampler_chain_add(g_state.sampler, llama_sampler_init_top_k(30));
    llama_sampler_chain_add(g_state.sampler, llama_sampler_init_top_p(0.95f, 1));
    llama_sampler_chain_add(g_state.sampler, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(g_state.sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    g_state.n_past = 0;
    LOGI("model loaded, n_ctx=%d threads=%d", n_ctx, n_threads);
    return 0;
}

/**
 * Tokenizes `prompt`, evaluates it, and arms the generation loop.
 * Returns 0 on success.
 */
JNIEXPORT jint JNICALL
Java_com_example_llama_LlamaBridge_nativeStartCompletion(
        JNIEnv * env, jobject, jstring prompt, jint max_tokens) {

    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_state.ctx || !g_state.model) return -1;

    const char * cprompt = env->GetStringUTFChars(prompt, nullptr);
    if (!cprompt) return -1;
    const std::string text(cprompt);
    env->ReleaseStringUTFChars(prompt, cprompt);

    const llama_vocab * vocab = llama_model_get_vocab(g_state.model);

    const int n_prompt = -llama_tokenize(
            vocab, text.c_str(), (int32_t) text.size(), nullptr, 0, true, true);
    if (n_prompt <= 0) return -2;

    std::vector<llama_token> tokens(n_prompt);
    if (llama_tokenize(vocab, text.c_str(), (int32_t) text.size(),
                       tokens.data(), (int32_t) tokens.size(), true, true) < 0) {
        return -2;
    }

    const uint32_t n_ctx = llama_n_ctx(g_state.ctx);
    if ((uint32_t) n_prompt + 8 >= n_ctx) {
        LOGE("prompt too long: %d tokens for context %u", n_prompt, n_ctx);
        return -4;
    }

    // --- KV cache prefix reuse -------------------------------------------------
    // Successive turns share a long prefix (system prompt + earlier messages).
    // Re-decoding it every time dominates time-to-first-token, so keep whatever
    // still matches and only evaluate the new suffix.
    size_t reuse = 0;
    const size_t max_reuse = std::min(g_state.cached.size(), (size_t) n_prompt);
    while (reuse < max_reuse && g_state.cached[reuse] == tokens[reuse]) {
        ++reuse;
    }

    // Never reuse the whole prompt: at least one token must be decoded to produce
    // logits for the first sampled token.
    if (reuse == (size_t) n_prompt) {
        reuse = (size_t) n_prompt - 1;
    }

    // Always drop everything at or after the divergence point. Doing this
    // unconditionally also covers the clamp above, where position n_prompt-1 may
    // still hold a stale entry from the previous turn.
    llama_memory_seq_rm(llama_get_memory(g_state.ctx), 0, (llama_pos) reuse, -1);

    g_state.n_past = (llama_pos) reuse;
    // Record only what the cache really holds; generated tokens are appended as
    // they are decoded in nativeNextToken().
    g_state.cached.assign(tokens.begin(), tokens.end());

    // Evaluate only the new suffix, in batches that respect n_batch.
    const int n_batch = 256;
    for (int i = (int) reuse; i < n_prompt; i += n_batch) {
        const int chunk = std::min(n_batch, n_prompt - i);
        llama_batch batch = llama_batch_get_one(tokens.data() + i, chunk);
        if (llama_decode(g_state.ctx, batch) != 0) {
            LOGE("llama_decode failed on prompt");
            // Cache no longer reflects the context; force a clean rebuild next time.
            llama_memory_clear(llama_get_memory(g_state.ctx), true);
            g_state.cached.clear();
            g_state.n_past = 0;
            return -3;
        }
        g_state.n_past += chunk;
    }
    LOGI("prompt %d tokens, reused %zu from cache", n_prompt, reuse);

    g_state.budget     = max_tokens;
    g_state.generating = true;
    g_state.utf8_carry.clear();
    return 0;
}

/**
 * Produces the next chunk of text.
 * Returns null when generation is finished (EOG, budget exhausted or context full).
 */
JNIEXPORT jstring JNICALL
Java_com_example_llama_LlamaBridge_nativeNextToken(JNIEnv * env, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_state.generating || !g_state.ctx) return nullptr;

    if (g_state.budget <= 0) {
        g_state.generating = false;
        return nullptr;
    }

    const llama_vocab * vocab = llama_model_get_vocab(g_state.model);

    const llama_token id = llama_sampler_sample(g_state.sampler, g_state.ctx, -1);

    if (llama_vocab_is_eog(vocab, id)) {
        g_state.generating = false;
        // The assistant turn ended cleanly; the cache is consistent and the next
        // message can reuse it as a prefix.
        return nullptr;
    }

    char buf[256];
    const int n = llama_token_to_piece(vocab, id, buf, sizeof(buf), 0, true);
    if (n < 0) {
        g_state.generating = false;
        return nullptr;
    }

    // Stitch onto any partial character left over from the previous token.
    g_state.utf8_carry.append(buf, n);
    const size_t hold = incomplete_utf8_tail(g_state.utf8_carry);
    std::string emit  = g_state.utf8_carry.substr(0, g_state.utf8_carry.size() - hold);
    g_state.utf8_carry = g_state.utf8_carry.substr(g_state.utf8_carry.size() - hold);

    // Feed the sampled token back in for the next step.
    llama_token next = id;
    llama_batch batch = llama_batch_get_one(&next, 1);
    if (llama_decode(g_state.ctx, batch) != 0) {
        g_state.generating = false;
        // Cache and context have diverged; rebuild from scratch next turn.
        llama_memory_clear(llama_get_memory(g_state.ctx), true);
        g_state.cached.clear();
        g_state.n_past = 0;
        return nullptr;
    }
    // Track generated tokens too, so the next turn's prefix match stays accurate.
    g_state.cached.push_back(id);
    g_state.n_past += 1;
    g_state.budget -= 1;

    if ((uint32_t) g_state.n_past + 4 >= llama_n_ctx(g_state.ctx)) {
        g_state.generating = false; // out of context window
    }

    if (emit.empty()) {
        // Mid-character: return an empty string so the caller keeps polling.
        return env->NewStringUTF("");
    }
    return env->NewStringUTF(emit.c_str());
}

JNIEXPORT void JNICALL
Java_com_example_llama_LlamaBridge_nativeStopCompletion(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    g_state.generating = false;
    g_state.utf8_carry.clear();
}

JNIEXPORT void JNICALL
Java_com_example_llama_LlamaBridge_nativeUnloadModel(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    free_context_locked();
}

JNIEXPORT jboolean JNICALL
Java_com_example_llama_LlamaBridge_nativeIsModelLoaded(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return g_state.model != nullptr && g_state.ctx != nullptr;
}

} // extern "C"
