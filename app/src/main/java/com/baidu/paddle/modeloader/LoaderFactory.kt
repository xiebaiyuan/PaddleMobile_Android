package com.baidu.paddle.modeloader

object LoaderFactory {
    fun buildLoader(type: ModelType): ModelLoader = when (type) {
        ModelType.mobilenet -> MobileNetModelLoaderImpl()
        ModelType.googlenet -> GoogleNetModelLoaderImpl()
        ModelType.mobilenet_ssd -> MobileNetSSDCombinedModelLoaderImpl()
        else -> MobileNetModelLoaderImpl()
    }
}