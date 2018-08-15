package com.baidu.paddle.modeloader

object LoaderFactory {
    fun buildLoader(type: ModelType): ModelLoader = when (type) {
        ModelType.mobilenet -> MobileNetModelLoaderImpl()
        ModelType.googlenet -> GoogleNetModelLoaderImpl()
        ModelType.mobilenet_ssd -> MobileNetSSDCombinedModelLoaderImpl()
        ModelType.mobilenet_combined -> MobileNetModelLoaderCombinedImpl()
        ModelType.mobilenet_combined_qualified -> MobileNetModelLoaderCombinedQualifiedImpl()
        ModelType.mobilenet_ssd_sep -> MobileNetSSDModelLoaderImpl()
        else -> MobileNetModelLoaderImpl()
    }
}