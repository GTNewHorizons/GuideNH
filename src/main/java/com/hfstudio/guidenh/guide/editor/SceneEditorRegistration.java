package com.hfstudio.guidenh.guide.editor;

/**
 * Reversible handle for one Scene Editor toolbar or dropdown registration.
 *
 * <p>
 * Calling {@link #close()} removes the exact registered object from its registry. The operation
 * is safe to call more than once and does not affect a later registration that reuses the same id.
 * Extensions should retain this handle for their own lifecycle cleanup.
 * </p>
 */
public interface SceneEditorRegistration extends AutoCloseable {

    /** Unregisters the associated contribution. */
    @Override
    void close();
}
