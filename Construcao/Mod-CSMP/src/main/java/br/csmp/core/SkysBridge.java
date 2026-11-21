package br.csmp.core;

import com.createskys.config.SkyConfig;
import com.createskys.config.SkySettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Unico ponto que toca nas classes do Create: Skys (so carregado se o mod existir). */
final class SkysBridge {
    private static final Logger LOG = LoggerFactory.getLogger("csmp");
    private static boolean avisou;

    private SkysBridge() {}

    static double coverage() {
        return SkyConfig.CLIENT.coverage.get();
    }

    static void setCoverage(double valor) {
        try {
            SkyConfig.CLIENT.coverage.set(Math.max(0.0, Math.min(1.0, valor)));
            SkySettings.refresh();
        } catch (Exception e) {
            if (!avisou) {
                avisou = true;
                LOG.warn("Nao consegui ajustar a cobertura de nuvens do Create: Skys", e);
            }
        }
    }
}