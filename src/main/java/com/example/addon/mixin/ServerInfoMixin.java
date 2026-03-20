package com.example.addon.mixin;

import com.example.addon.TranslateAPI;
import com.example.addon.modules.TranslateChat;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerInfo.class)
public class ServerInfoMixin {

    @Shadow public Text label;

    /**
     * ServerInfo.copyWithResolvedAddress() is called after the ping completes
     * and the MOTD label has been populated. We translate it here.
     * Falls back: if label is set before this point, we catch it in the
     * populateFromServerPing path by targeting the method that sets label.
     */
    @Inject(method = "copyWithResolvedAddress", at = @At("HEAD"))
    private void onCopyWithResolvedAddress(CallbackInfo ci) {
        translate();
    }

    /**
     * Also hook populateFromServerPing so we catch the label at the point
     * it is written from the server's status response.
     */
    @Inject(method = "populateFromServerPing", at = @At("TAIL"))
    private void onPopulateFromServerPing(CallbackInfo ci) {
        translate();
    }

    private void translate() {
        TranslateChat module = Modules.get() != null ? Modules.get().get(TranslateChat.class) : null;
        if (module == null || !module.isActive()) return;
        if (label == null) return;

        String text = label.getString();
        if (text.isBlank()) return;

        ServerInfo self = (ServerInfo) (Object) this;
        TranslateAPI.translateAsync(text, module.getTargetCode(), result -> {
            if (result == null || result.translation().isBlank()) return;
            self.label = Text.literal(result.translation());
        });
    }
}
