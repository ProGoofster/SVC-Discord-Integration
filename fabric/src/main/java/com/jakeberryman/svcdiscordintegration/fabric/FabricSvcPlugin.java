package com.jakeberryman.svcdiscordintegration.fabric;

import com.jakeberryman.svcdiscordintegration.voicechat.SvcPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;

public class FabricSvcPlugin extends SvcPlugin {

    @Override
    public void initialize(VoicechatApi api) {
        // Set the platform helper for Fabric
        setPlatformHelper(new FabricPlatformHelper());
        super.initialize(api);
    }
}
