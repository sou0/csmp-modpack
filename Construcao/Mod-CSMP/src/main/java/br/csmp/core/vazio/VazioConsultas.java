package br.csmp.core.vazio;

/** Consultas publicas sobre o plano do Vazio (para comandos de admin e testes). */
public final class VazioConsultas {
    private VazioConsultas() {}

    /** Ponto do meio de rua mais proximo de (x, z), procurando ate {@code alcance} blocos; null se nao houver. */
    public static int[] ruaMaisPerto(long semente, int x, int z, int alcance) {
        int[] melhor = null;
        double menor = Double.MAX_VALUE;
        for (VazioMapa.Rua rua : VazioMapa.ruas(semente, x - alcance, z - alcance, x + alcance, z + alcance)) {
            for (int i = 0; i <= 64; i++) {
                double[] p = rua.ponto(rua.t0() + (rua.t1() - rua.t0()) * i / 64.0D);
                double d = Math.hypot(p[0] - x, p[1] - z);
                if (d < menor) {
                    menor = d;
                    melhor = new int[] {(int) Math.floor(p[0]), (int) Math.floor(p[1])};
                }
            }
        }
        return melhor;
    }

    /** Centro do lote mais proximo do tipo pedido ("casa", "estatua", "praca", "capela", "loja", "ruina"). */
    public static int[] loteMaisPerto(long semente, int x, int z, int alcance, String tipo) {
        int alvo = switch (tipo) {
            case "estatua" -> VazioMapa.ESTATUA;
            case "praca" -> VazioMapa.PRACA;
            case "capela" -> VazioMapa.CAPELA;
            case "loja" -> VazioMapa.LOJA;
            case "ruina" -> VazioMapa.RUINA;
            default -> VazioMapa.CASA;
        };
        int[] melhor = null;
        double menor = Double.MAX_VALUE;
        for (VazioMapa.Lote l : VazioMapa.lotes(semente, x - alcance, z - alcance, x + alcance, z + alcance)) {
            if (l.tipo() != alvo) {
                continue;
            }
            double d = Math.hypot(l.x() - x, l.z() - z);
            if (d < menor) {
                menor = d;
                melhor = new int[] {l.x(), l.z()};
            }
        }
        return melhor;
    }
}
