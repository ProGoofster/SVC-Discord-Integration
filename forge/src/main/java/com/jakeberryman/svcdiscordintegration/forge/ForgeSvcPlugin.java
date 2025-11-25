package com.jakeberryman.svcdiscordintegration.forge;

import com.jakeberryman.svcdiscordintegration.voicechat.SvcPlugin;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;

@ForgeVoicechatPlugin
public class ForgeSvcPlugin extends SvcPlugin {

    @Override
    public void initialize(VoicechatApi api) {
        // Set the platform helper for Forge
        setPlatformHelper(new ForgePlatformHelper());
        super.initialize(api);
    }
}
