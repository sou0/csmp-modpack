package br.csmp.core.npc;

import br.csmp.core.CsmpMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/**
 * Missoes e falas do Profeta, lidas de assets/csmp/profeta/profeta.json (o escritor pode editar,
 * e um resourcepack pode trocar). Se o arquivo faltar ou estiver quebrado, usa um texto padrao.
 */
public record ProfetaDados(List<Missao> missoes, List<String> conversa) {

    public record Missao(String titulo, String descricao, String estado) {}

    public static ProfetaDados carregar() {
        try {
            var recurso = Minecraft.getInstance().getResourceManager()
                    .getResource(ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "profeta/profeta.json"));
            if (recurso.isPresent()) {
                try (Reader leitor = recurso.get().openAsReader()) {
                    JsonObject raiz = JsonParser.parseReader(leitor).getAsJsonObject();
                    List<Missao> missoes = new ArrayList<>();
                    JsonArray listaMissoes = GsonHelper.getAsJsonArray(raiz, "missoes", new JsonArray());
                    for (JsonElement elemento : listaMissoes) {
                        JsonObject m = elemento.getAsJsonObject();
                        missoes.add(new Missao(
                                GsonHelper.getAsString(m, "titulo", "?"),
                                GsonHelper.getAsString(m, "descricao", ""),
                                GsonHelper.getAsString(m, "estado", "disponivel")));
                    }
                    List<String> conversa = new ArrayList<>();
                    JsonArray listaConversa = GsonHelper.getAsJsonArray(raiz, "conversa", new JsonArray());
                    for (JsonElement elemento : listaConversa) {
                        conversa.add(elemento.getAsString());
                    }
                    if (conversa.isEmpty()) {
                        conversa = padrao().conversa();
                    }
                    return new ProfetaDados(missoes, conversa);
                }
            }
        } catch (Exception erro) {
            // arquivo ausente ou invalido: cai no texto padrao abaixo
        }
        return padrao();
    }

    public static ProfetaDados padrao() {
        return new ProfetaDados(
                List.of(new Missao("Sem missoes", "Nenhuma missao foi cadastrada ainda.", "bloqueada")),
                List.of("..."));
    }
}
