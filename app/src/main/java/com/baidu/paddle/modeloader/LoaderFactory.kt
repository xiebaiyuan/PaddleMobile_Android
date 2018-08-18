package com.baidu.paddle.modeloader

object LoaderFactory {
    fun buildLoader(type: ModelType): ModelLoader = when (type) {
        ModelType.mobilenet -> MobileNetModelLoaderImpl()
        ModelType.googlenet -> GoogleNetModelLoaderImpl()
        ModelType.mobilenet_ssd -> MobileNetSSDCombinedModelLoaderImpl()
        ModelType.mobilenet_combined -> MobileNetModelLoaderCombinedImpl()
        ModelType.mobilenet_combined_qualified -> MobileNetModelLoaderCombinedQualifiedImpl()
        ModelType.googlenet_combine_quali -> GoogleNetModelCombinedQualiLoaderImpl()
        ModelType.genet_combine -> GEnetModelLoaderCombinedImpl()
        else -> MobileNetModelLoaderImpl()
    }
}