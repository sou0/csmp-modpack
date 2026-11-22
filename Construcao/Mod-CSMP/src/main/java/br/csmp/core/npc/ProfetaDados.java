package br.csmp.core.npc;

import br.csmp.core.CsmpMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/**
 * Missoes e dialogo do Profeta, lidos de assets/csmp/profeta/profeta.json (o escritor pode editar,
 * e um resourcepack pode trocar). Se o arquivo faltar ou estiver quebrado, usa um texto padrao.
 *
 * O dialogo e uma arvore: cada no tem uma fala e opcoes; cada opcao aponta para outro no
 * ("vai": id do no, "menu" volta ao menu inicial, "fechar" fecha a tela).
 */
public record ProfetaDados(String saudacao, String titulo, List<Missao> missoes, Map<String, No> dialogo) {

    public record Missao(String titulo, String descricao, String objetivo, String recompensa, String estado) {}

    public record Resposta(String texto, String vai) {}

    public record No(String fala, List<Resposta> opcoes) {}

    public static ProfetaDados carregar() {
        try {
            var recurso = Minecraft.getInstance().getResourceManager()
                    .getResource(ResourceLocation.fromNamespaceAndPath(CsmpMod.ID, "profeta/profeta.json"));
            if (recurso.isPresent()) {
                try (Reader leitor = recurso.get().openAsReader()) {
                    return ler(JsonParser.parseReader(leitor).getAsJsonObject());
                }
            }
        } catch (Exception erro) {
            // arquivo ausente ou invalido: cai no texto padrao abaixo
        }
        return padrao();
    }

    private static ProfetaDados ler(JsonObject raiz) {
        ProfetaDados base = padrao();
        String saudacao = GsonHelper.getAsString(raiz, "saudacao", base.saudacao());
        String titulo = GsonHelper.getAsString(raiz, "titulo", base.titulo());

        List<Missao> missoes = new ArrayList<>();
        for (JsonElement elemento : GsonHelper.getAsJsonArray(raiz, "missoes", new JsonArray())) {
            JsonObject m = elemento.getAsJsonObject();
            missoes.add(new Missao(
                    GsonHelper.getAsString(m, "titulo", "?"),
                    GsonHelper.getAsString(m, "descricao", ""),
                    GsonHelper.getAsString(m, "objetivo", ""),
                    GsonHelper.getAsString(m, "recompensa", ""),
                    GsonHelper.getAsString(m, "estado", "disponivel")));
        }

        Map<String, No> dialogo = new LinkedHashMap<>();
        JsonObject arvore = GsonHelper.getAsJsonObject(raiz, "dialogo", new JsonObject());
        for (Map.Entry<String, JsonElement> entrada : arvore.entrySet()) {
            JsonObject no = entrada.getValue().getAsJsonObject();
            List<Resposta> opcoes = new ArrayList<>();
            for (JsonElement elemento : GsonHelper.getAsJsonArray(no, "opcoes", new JsonArray())) {
                JsonObject o = elemento.getAsJsonObject();
                opcoes.add(new Resposta(GsonHelper.getAsString(o, "texto", "..."),
                        GsonHelper.getAsString(o, "vai", "menu")));
            }
            dialogo.put(entrada.getKey(), new No(GsonHelper.getAsString(no, "fala", "..."), opcoes));
        }
        if (!dialogo.containsKey("inicio")) {
            dialogo = base.dialogo();
        }
        return new ProfetaDados(saudacao, titulo, missoes, dialogo);
    }

    public static ProfetaDados padrao() {
        Map<String, No> dialogo = new LinkedHashMap<>();
        dialogo.put("inicio", new No("...", List.of(new Resposta("Voltar", "menu"))));
        return new ProfetaDados("...", "",
                List.of(new Missao("Sem missoes", "Nenhuma missao foi cadastrada ainda.", "", "", "bloqueada")),
                dialogo);
    }
}
