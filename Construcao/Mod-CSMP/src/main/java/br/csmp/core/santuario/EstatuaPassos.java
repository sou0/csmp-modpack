package br.csmp.core.santuario;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Lista de itens (na ordem certa) que montam a estatua. Fica em config/csmp-estatua.json para o escritor
 * editar sem mexer no mod; se o arquivo nao existir, e criado com a lista inicial.
 * Cada item e colocado um por vez (1 unidade) e as partes aparecem na ordem: pernas, torso, bracos, cabeca.
 */
public final class EstatuaPassos {
    private static final List<String> PADRAO = List.of(
            "minecraft:stone", "minecraft:bone",
            "minecraft:smooth_stone", "minecraft:golden_apple",
            "minecraft:cobblestone", "minecraft:blaze_rod",
            "minecraft:skeleton_skull", "minecraft:fermented_spider_eye");

    private static List<String> passos;

    private EstatuaPassos() {}

    public static synchronized List<String> passos() {
        if (passos == null) {
            passos = carregar();
        }
        return passos;
    }

    /** Mascara de partes visiveis (EstatuaEntity.PERNAS...CABECA) para uma quantidade de itens ja colocados. */
    public static int mascaraPara(int colocados) {
        int total = passos().size();
        int mascara = 0;
        for (int parte = 0; parte < 4; parte++) {
            int necessario = (int) Math.ceil((parte + 1) * total / 4.0D);
            if (colocados >= necessario) {
                mascara |= 1 << parte;
            }
        }
        return mascara;
    }

    private static List<String> carregar() {
        Path arquivo = FMLPaths.CONFIGDIR.get().resolve("csmp-estatua.json");
        Gson gson = new Gson();
        try {
            if (Files.exists(arquivo)) {
                JsonObject raiz = gson.fromJson(Files.readString(arquivo, StandardCharsets.UTF_8), JsonObject.class);
                JsonArray lista = raiz.getAsJsonArray("passos");
                List<String> lido = new ArrayList<>();
                for (JsonElement e : lista) {
                    lido.add(e.getAsString());
                }
                if (lido.size() >= 4) {
                    return List.copyOf(lido);
                }
            } else {
                JsonObject raiz = new JsonObject();
                JsonArray lista = new JsonArray();
                PADRAO.forEach(lista::add);
                raiz.addProperty("comentario", "Itens da estatua, UM de cada vez, na ordem certa. No minimo 4. "
                        + "As partes aparecem na ordem: pernas, torso, bracos, cabeca.");
                raiz.add("passos", lista);
                Files.createDirectories(arquivo.getParent());
                Files.writeString(arquivo, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(raiz),
                        StandardCharsets.UTF_8);
            }
        } catch (IOException | RuntimeException erro) {
            // arquivo quebrado: usa a lista inicial
        }
        return PADRAO;
    }
}
