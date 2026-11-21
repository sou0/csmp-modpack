package br.csmp.core;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Cliente: ajusta a cobertura de nuvens do Create: Skys conforme o caos.
 *  0-24%  poucas nuvens | 25-34% meio nublado | 35-49% nublado | 50%+ cobertura de tempestade
 * A mudanca e gradual. Ao sair do servidor o valor original do jogador volta.
 */
@EventBusSubscriber(modid = CsmpMod.ID, value = Dist.CLIENT)
public final class ClientCaos {
    private static volatile int caos = -1; // -1 = ainda nao recebi do servidor
    private static double atual = Double.NaN;
    private static double original = Double.NaN;
    private static int contador;

    private ClientCaos() {}

    public static void set(int valor) {
        caos = valor;
    }

    static double alvo(int c) {
        if (c < 25) return 0.35;
        if (c < 35) return 0.55;
        if (c < 50) return 0.78;
        return 0.85;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (caos < 0 || !ModList.get().isLoaded("createskys")) return;
        if (++contador % 10 != 0) return; // 2x por segundo
        if (Double.isNaN(atual)) {
            original = SkysBridge.coverage();
            atual = original;
        }
        double dif = alvo(caos) - atual;
        if (Math.abs(dif) < 0.01) return;
        atual += Math.signum(dif) * Math.min(Math.abs(dif), 0.03);
        SkysBridge.setCoverage(atual);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        caos = -1;
        if (!Double.isNaN(original) && ModList.get().isLoaded("createskys")) {
            SkysBridge.setCoverage(original);
        }
        atual = Double.NaN;
        original = Double.NaN;
    }
}