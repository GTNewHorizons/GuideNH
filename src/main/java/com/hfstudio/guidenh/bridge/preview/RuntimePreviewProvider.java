package com.hfstudio.guidenh.bridge.preview;

public interface RuntimePreviewProvider extends AutoCloseable {

    String capability();

    PreviewSearchResult search(PreviewSearchQuery query);

    PreviewResolveResult resolve(PreviewResolveQuery query);

    @Override
    default void close() {}
}
