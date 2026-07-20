plugins {
    id("com.android.asset-pack") version "8.13.1"
}

assetPack {
    packName.set("model_pack")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}