package br.csmp.core.vazio;

import br.csmp.core.CsmpRegistro;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * As construcoes das ruas do Vazio: casas, lojas, capelas, pracas com poco e ruinas, cada uma com medidas, paleta e
 * estragos sorteados (nunca duas iguais). As armadilhas ficam DENTRO delas: assoalho falso com pontas embaixo, fio na
 * porta que cega e prende, placa de pressao com dispenser escondido na parede, piso falso na praca.
 * Coordenadas locais: {@code a} vai para o lado, {@code b} para o fundo (b negativo = frente, virada para a rua).
 */
final class ConstrucoesVazio {
    private ConstrucoesVazio() {}

    private static BlockState b(Block bloco) {
        return bloco.defaultBlockState();
    }

    /** Paleta de uma casa. O piso e sempre de carvalho escuro, para o assoalho falso ficar identico. */
    private record Paleta(BlockState parede, BlockState rachada, BlockState canto, Block telhado, BlockState telhadoLaje,
                          BlockState janela, Block porta) {}

    private static final Paleta[] PALETAS = {
            new Paleta(b(Blocks.DARK_OAK_PLANKS), b(Blocks.STRIPPED_DARK_OAK_WOOD), b(Blocks.DARK_OAK_LOG),
                    Blocks.DEEPSLATE_TILE_STAIRS, b(Blocks.DEEPSLATE_TILE_SLAB), b(Blocks.BLACK_STAINED_GLASS_PANE), Blocks.DARK_OAK_DOOR),
            new Paleta(b(Blocks.DEEPSLATE_BRICKS), b(Blocks.CRACKED_DEEPSLATE_BRICKS), b(Blocks.POLISHED_DEEPSLATE),
                    Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, b(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB), b(Blocks.RED_STAINED_GLASS_PANE), Blocks.SPRUCE_DOOR),
            new Paleta(b(Blocks.WHITE_CONCRETE), b(Blocks.LIGHT_GRAY_CONCRETE), b(Blocks.SPRUCE_LOG),
                    Blocks.DARK_OAK_STAIRS, b(Blocks.DARK_OAK_SLAB), b(Blocks.GLASS_PANE), Blocks.SPRUCE_DOOR),
            new Paleta(b(Blocks.MUD_BRICKS), b(Blocks.PACKED_MUD), b(Blocks.STRIPPED_SPRUCE_LOG),
                    Blocks.SPRUCE_STAIRS, b(Blocks.SPRUCE_SLAB), b(Blocks.BROWN_STAINED_GLASS_PANE), Blocks.DARK_OAK_DOOR),
    };

    // ------------------------------------------------------------------ obra: helper com rotacao

    /** Ferramenta de construcao rotacionada para a frente do lote, que guarda o que precisa se ligar aos vizinhos. */
    static final class Obra {
        final WorldGenLevel n;
        final BlockPos centro;
        final Direction frente;
        final Direction direita;
        final Random r;
        final List<BlockPos> ajustar = new ArrayList<>();

        Obra(WorldGenLevel n, BlockPos centro, Direction frente, long semente) {
            this.n = n;
            this.centro = centro;
            this.frente = frente;
            this.direita = frente.getClockWise();
            this.r = new Random(semente);
        }

        BlockPos p(int a, int y, int bb) {
            return this.centro.relative(this.direita, a).relative(this.frente, -bb).above(y);
        }

        /** Direcao local -> mundo (NORTH = frente, EAST = direita, SOUTH = fundo, WEST = esquerda). */
        Direction d(Direction local) {
            return switch (local) {
                case EAST -> this.direita;
                case SOUTH -> this.frente.getOpposite();
                case WEST -> this.direita.getOpposite();
                default -> this.frente;
            };
        }

        void set(int a, int y, int bb, BlockState estado) {
            this.n.setBlock(p(a, y, bb), estado, 2);
        }

        void forma(int a, int y, int bb, BlockState estado) {
            set(a, y, bb, estado);
            this.ajustar.add(p(a, y, bb));
        }

        void caixa(int a1, int y1, int b1, int a2, int y2, int b2, BlockState estado) {
            for (int a = Math.min(a1, a2); a <= Math.max(a1, a2); a++) {
                for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                    for (int bb = Math.min(b1, b2); bb <= Math.max(b1, b2); bb++) {
                        set(a, y, bb, estado);
                    }
                }
            }
        }

        BlockState escada(Block bloco, Direction local, boolean deCabeca) {
            return bloco.defaultBlockState().setValue(StairBlock.FACING, d(local))
                    .setValue(StairBlock.HALF, deCabeca ? Half.TOP : Half.BOTTOM);
        }

        /** Abre espaco: tira arvores e mato da area (e um pouco alem) ate a altura dada. */
        void limpar(int ra, int rb, int altura) {
            caixa(-ra, 0, -rb, ra, altura, rb, b(Blocks.AIR));
        }

        void ajustarFormas() {
            for (BlockPos q : this.ajustar) {
                BlockState estado = this.n.getBlockState(q);
                for (Direction dir : Direction.values()) {
                    BlockPos v = q.relative(dir);
                    estado = estado.updateShape(dir, this.n.getBlockState(v), this.n, q, v);
                }
                this.n.setBlock(q, estado, 2);
            }
        }

        /** Dispenser com flechas: a block entity ainda nao existe na geracao, entao o conteudo vai como NBT no chunk. */
        void dispenserComFlechas(int a, int y, int bb, Direction local) {
            set(a, y, bb, b(Blocks.DISPENSER).setValue(DispenserBlock.FACING, d(local)));
            BlockPos q = p(a, y, bb);
            CompoundTag dados = new CompoundTag();
            dados.putString("id", "minecraft:dispenser");
            dados.putInt("x", q.getX());
            dados.putInt("y", q.getY());
            dados.putInt("z", q.getZ());
            ListTag itens = new ListTag();
            CompoundTag flecha = new CompoundTag();
            flecha.putByte("Slot", (byte) 4);
            flecha.putString("id", "minecraft:arrow");
            flecha.putInt("count", 32);
            itens.add(flecha);
            dados.put("Items", itens);
            this.n.getChunk(q).setBlockEntityNbt(dados);
        }

        /** Buraco escondido: o piso fica falso (sem colisao) e embaixo ha 2 blocos de queda ate pontas de pedra. */
        void buracoFalso(int a, int bb, BlockState pisoFalso) {
            set(a, -3, bb, b(Blocks.POINTED_DRIPSTONE).setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.UP));
            set(a, -2, bb, b(Blocks.AIR));
            set(a, -1, bb, pisoFalso);
        }

        /** Fio esticado entre dois ganchos presos as paredes (ganchos em a1 e a2, fio entre eles), na linha bb. */
        void fio(int a1, int a2, int bb, int y) {
            BlockState gancho = b(Blocks.TRIPWIRE_HOOK).setValue(TripWireHookBlock.ATTACHED, true);
            set(a1, y, bb, gancho.setValue(TripWireHookBlock.FACING, d(Direction.EAST)));
            set(a2, y, bb, gancho.setValue(TripWireHookBlock.FACING, d(Direction.WEST)));
            boolean eixoX = d(Direction.EAST).getAxis() == Direction.Axis.X;
            BlockState fio = b(Blocks.TRIPWIRE).setValue(TripWireBlock.ATTACHED, true)
                    .setValue(eixoX ? TripWireBlock.EAST : TripWireBlock.NORTH, true)
                    .setValue(eixoX ? TripWireBlock.WEST : TripWireBlock.SOUTH, true);
            for (int a = a1 + 1; a < a2; a++) {
                set(a, y, bb, fio);
            }
        }
    }

    // ------------------------------------------------------------------ entrada

    static void construir(WorldGenLevel n, VazioMapa.Lote lote) {
        BlockPos centro = new BlockPos(lote.x(), VazioMapa.Y, lote.z());
        switch (lote.tipo()) {
            case VazioMapa.CASA -> casa(new Obra(n, centro, lote.frente(), lote.semente()), false);
            case VazioMapa.LOJA -> casa(new Obra(n, centro, lote.frente(), lote.semente()), true);
            case VazioMapa.CAPELA -> capela(new Obra(n, centro, lote.frente(), lote.semente()));
            case VazioMapa.PRACA -> praca(new Obra(n, centro, lote.frente(), lote.semente()));
            case VazioMapa.RUINA -> ruina(new Obra(n, centro, lote.frente(), lote.semente()));
            case VazioMapa.MONOLITO -> EstruturaVazioFeature.monolito(n, centro);
            case VazioMapa.BALANCO -> EstruturaVazioFeature.balanco(n, centro);
            case VazioMapa.ESCADA -> EstruturaVazioFeature.escada(n, centro);
            case VazioMapa.PORTAS -> EstruturaVazioFeature.portas(n, centro);
            case VazioMapa.FONTE -> EstruturaVazioFeature.fonte(n, centro);
            case VazioMapa.ESTATUA -> br.csmp.core.santuario.EstatuaFeature.construir(n, centro, true);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ casa e loja

    /**
     * Casa (ou loja) de 1 ou 2 andares: paredes com cantos de tronco, janelas (algumas quebradas), porta virada para a
     * rua, telhado de duas aguas, chamine as vezes, moveis por dentro e estragos. Loja: sempre 2 andares, vitrine,
     * toldo e balcao. Uma armadilha sorteada fica escondida dentro.
     */
    private static void casa(Obra o, boolean loja) {
        Random r = o.r;
        Paleta p = PALETAS[r.nextInt(PALETAS.length)];
        int hw = loja ? 4 : 3 + r.nextInt(2);
        int hd = 3 + r.nextInt(2);
        int andares = loja || r.nextInt(2) == 0 ? 2 : 1;
        int h = 4;
        int topo = andares * h;
        BlockState piso = b(Blocks.DARK_OAK_PLANKS);
        o.limpar(hw + 2, hd + 2, topo + hd + 6);
        o.caixa(-hw, -1, -hd, hw, -1, hd, piso);
        for (int f = 0; f < andares; f++) {
            int y0 = f * h;
            for (int a = -hw; a <= hw; a++) {
                for (int bb = -hd; bb <= hd; bb++) {
                    boolean borda = Math.abs(a) == hw || Math.abs(bb) == hd;
                    if (!borda) {
                        continue;
                    }
                    boolean canto = Math.abs(a) == hw && Math.abs(bb) == hd;
                    for (int y = y0; y < y0 + h; y++) {
                        if (canto) {
                            o.set(a, y, bb, p.canto().hasProperty(RotatedPillarBlock.AXIS) ? p.canto().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y) : p.canto());
                        } else {
                            o.set(a, y, bb, r.nextInt(14) == 0 ? p.rachada() : p.parede());
                        }
                    }
                    // janelas no meio das paredes, alternadas; algumas quebradas
                    boolean vitrine = loja && f == 0 && bb == -hd && Math.abs(a) >= 2 && Math.abs(a) < hw;
                    boolean janela = !canto && ((Math.abs(a) == hw && Math.abs(bb) < hd - 1 && Math.floorMod(bb, 2) == 0)
                            || (Math.abs(bb) == hd && Math.abs(a) < hw - 1 && Math.floorMod(a, 2) == 1 && a != 0));
                    if (vitrine || janela) {
                        for (int y = y0 + 1; y <= y0 + 2; y++) {
                            if (r.nextInt(5) == 0) {
                                o.set(a, y, bb, b(Blocks.AIR));
                            } else {
                                o.forma(a, y, bb, p.janela());
                            }
                        }
                    }
                }
            }
            if (f > 0) {
                o.caixa(-hw + 1, y0 - 1, -hd + 1, hw - 1, y0 - 1, hd - 1, piso);
            }
        }
        // escada de mao para o andar de cima
        if (andares == 2) {
            BlockState escadaMao = b(Blocks.LADDER).setValue(LadderBlock.FACING, o.d(Direction.WEST));
            for (int y = 0; y < h; y++) {
                o.set(hw - 1, y, hd - 1, escadaMao);
            }
        }
        // porta virada para a rua
        o.set(0, 0, -hd, b(Blocks.AIR));
        o.set(0, 1, -hd, b(Blocks.AIR));
        boolean aberta = r.nextInt(3) == 0;
        BlockState porta = p.porta().defaultBlockState().setValue(DoorBlock.FACING, o.d(Direction.NORTH))
                .setValue(DoorBlock.OPEN, aberta);
        o.set(0, 0, -hd, porta.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        o.set(0, 1, -hd, porta.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        // telhado de duas aguas (cumeeira ao longo da largura)
        for (int k = 0; ; k++) {
            int bFrente = -hd - 1 + k;
            int bFundo = hd + 1 - k;
            int y = topo + k;
            if (bFrente > bFundo) {
                break;
            }
            for (int a = -hw - 1; a <= hw + 1; a++) {
                if (bFrente == bFundo) {
                    o.set(a, y, bFrente, r.nextInt(10) == 0 ? b(Blocks.AIR) : p.telhadoLaje());
                } else {
                    if (r.nextInt(9) != 0) {
                        o.set(a, y, bFrente, o.escada(p.telhado(), Direction.SOUTH, false));
                    }
                    if (r.nextInt(9) != 0) {
                        o.set(a, y, bFundo, o.escada(p.telhado(), Direction.NORTH, false));
                    }
                }
            }
            // oitao (parede triangular) nas laterais
            for (int bb = bFrente + 1; bb < bFundo; bb++) {
                o.set(-hw, y, bb, p.parede());
                o.set(hw, y, bb, p.parede());
            }
        }
        if (r.nextInt(5) < 2) {
            for (int y = topo - 1; y <= topo + hd + 2; y++) {
                o.set(-hw + 1, y, hd - 1, b(Blocks.BRICKS));
            }
            o.set(-hw + 1, topo + hd + 3, hd - 1, b(Blocks.CAMPFIRE).setValue(CampfireBlock.LIT, false));
        }
        // moveis
        o.set(-hw + 1, 0, hd - 1, b(Blocks.RED_BED).setValue(BedBlock.FACING, o.d(Direction.SOUTH)).setValue(BedBlock.PART, BedPart.HEAD));
        o.set(-hw + 1, 0, hd - 2, b(Blocks.RED_BED).setValue(BedBlock.FACING, o.d(Direction.SOUTH)).setValue(BedBlock.PART, BedPart.FOOT));
        o.forma(1, 0, 0, b(Blocks.DARK_OAK_FENCE));
        o.set(1, 1, 0, b(Blocks.DARK_OAK_PRESSURE_PLATE));
        o.set(1, 0, -1, o.escada(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false));
        o.set(-hw + 1, 0, -hd + 1, b(Blocks.BARREL));
        o.set(hw - 1, 0, -hd + 1, b(Blocks.BOOKSHELF));
        o.set(1, 2, 0, b(Blocks.LANTERN));
        if (r.nextBoolean()) {
            o.set(-hw + 1, 2, -hd + 1, b(Blocks.COBWEB));
        }
        if (andares == 2) {
            o.set(-hw + 1, h, -hd + 1, b(Blocks.CHEST));
            o.set(0, h, 0, b(Blocks.GRAY_CARPET));
            o.set(hw - 2, h, -hd + 1, b(Blocks.SOUL_LANTERN));
        }
        if (loja) {
            // balcao e prateleiras
            for (int a = -hw + 2; a <= hw - 2; a++) {
                if (a != 0) {
                    o.set(a, 0, -hd + 2, o.escada(Blocks.DARK_OAK_STAIRS, Direction.NORTH, true));
                }
            }
            for (int bb = -hd + 3; bb <= hd - 1; bb++) {
                o.set(hw - 1, 0, bb, b(Blocks.BARREL));
                o.set(hw - 1, 1, bb, r.nextBoolean() ? b(Blocks.BARREL) : b(Blocks.AIR));
            }
            // toldo vermelho escuro sobre a vitrine
            for (int a = -hw; a <= hw; a++) {
                o.set(a, 3, -hd - 1, b(Blocks.CRIMSON_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP));
            }
        }
        // armadilha escondida
        int sorteio = r.nextInt(10);
        if (sorteio < 4) {
            BlockState falso = CsmpRegistro.ASSOALHO_FALSO.get().defaultBlockState();
            for (int a = hw - 2; a <= hw - 1; a++) {
                for (int bb = 1; bb <= 2; bb++) {
                    o.buracoFalso(a, bb, falso);
                }
            }
        } else if (sorteio < 7) {
            o.fio(-hw + 1, hw - 1, -hd + 1, 0);
        } else if (sorteio < 9) {
            o.set(hw - 1, 0, -1, b(Blocks.DARK_OAK_PRESSURE_PLATE));
            o.dispenserComFlechas(hw, 0, -1, Direction.WEST);
        }
        o.ajustarFormas();
    }

    // ------------------------------------------------------------------ capela

    /** Capela: nave comprida com bancos, vitrais vermelhos, altar com velas no fundo e campanario na frente. */
    private static void capela(Obra o) {
        Random r = o.r;
        int hw = 3;
        int hd = 6;
        int h = 6;
        BlockState parede = b(Blocks.DEEPSLATE_BRICKS);
        o.limpar(hw + 2, hd + 3, h + hw + 10);
        o.caixa(-hw, -1, -hd, hw, -1, hd, b(Blocks.POLISHED_DEEPSLATE));
        for (int a = -hw; a <= hw; a++) {
            for (int bb = -hd; bb <= hd; bb++) {
                if (Math.abs(a) != hw && Math.abs(bb) != hd) {
                    continue;
                }
                for (int y = 0; y < h; y++) {
                    o.set(a, y, bb, r.nextInt(12) == 0 ? b(Blocks.CRACKED_DEEPSLATE_BRICKS) : parede);
                }
                if (Math.abs(a) == hw && Math.abs(bb) < hd - 1 && Math.floorMod(bb, 2) == 0) {
                    for (int y = 1; y <= 4; y++) {
                        o.forma(a, y, bb, b(Blocks.RED_STAINED_GLASS_PANE));
                    }
                }
            }
        }
        for (int y = 0; y <= 2; y++) {
            o.set(0, y, -hd, b(Blocks.AIR));
        }
        // telhado inclinado ingreme
        for (int k = 0; k <= hw + 1; k++) {
            int y = h + k;
            for (int bb = -hd - 1; bb <= hd + 1; bb++) {
                if (-hw - 1 + k == hw + 1 - k) {
                    o.set(0, y, bb, b(Blocks.DEEPSLATE_TILE_SLAB));
                } else if (-hw - 1 + k < hw + 1 - k) {
                    o.set(-hw - 1 + k, y, bb, o.escada(Blocks.DEEPSLATE_TILE_STAIRS, Direction.EAST, false));
                    o.set(hw + 1 - k, y, bb, o.escada(Blocks.DEEPSLATE_TILE_STAIRS, Direction.WEST, false));
                }
            }
            for (int a = -hw + 1 + k; a <= hw - 1 - k; a++) {
                o.set(a, y, -hd, parede);
                o.set(a, y, hd, parede);
            }
        }
        // campanario sobre a porta
        for (int y = h; y <= h + hw + 4; y++) {
            o.set(0, y, -hd, parede);
        }
        o.set(0, h + hw + 3, -hd - 1, b(Blocks.BELL));
        o.set(0, h + hw + 5, -hd, b(Blocks.DEEPSLATE_TILE_SLAB));
        // bancos, corredor e altar
        for (int bb = -hd + 2; bb <= hd - 3; bb += 2) {
            for (int a : new int[] {-2, -1, 1, 2}) {
                if (r.nextInt(6) != 0) {
                    o.set(a, 0, bb, o.escada(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false));
                }
            }
        }
        for (int bb = -hd + 1; bb <= hd - 2; bb++) {
            o.set(0, 0, bb, b(Blocks.RED_CARPET));
        }
        o.caixa(-1, 0, hd - 1, 1, 0, hd - 1, b(Blocks.CHISELED_POLISHED_BLACKSTONE));
        o.set(-1, 1, hd - 1, b(Blocks.BLACK_CANDLE).setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
        o.set(1, 1, hd - 1, b(Blocks.RED_CANDLE).setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true));
        // armadilha: fio logo depois da porta
        if (r.nextBoolean()) {
            o.fio(-hw + 1, hw - 1, -hd + 1, 0);
        }
        o.ajustarFormas();
    }

    // ------------------------------------------------------------------ praca com poco

    /** Praca de pedra com um poco coberto no meio, bancos, postes nos cantos e lajotas falsas perto do poco. */
    private static void praca(Obra o) {
        Random r = o.r;
        int raio = 6;
        o.limpar(raio + 2, raio + 2, 12);
        for (int a = -raio; a <= raio; a++) {
            for (int bb = -raio; bb <= raio; bb++) {
                double d = Math.sqrt(a * a + bb * bb);
                if (d > raio + 0.5D) {
                    continue;
                }
                BlockState piso = d > raio - 1.0D ? b(Blocks.POLISHED_BLACKSTONE_BRICKS)
                        : (Math.floorMod(a + bb, 2) == 0 ? b(Blocks.STONE_BRICKS) : b(Blocks.CRACKED_STONE_BRICKS));
                o.set(a, -1, bb, r.nextInt(9) == 0 ? b(Blocks.MOSSY_STONE_BRICKS) : piso);
            }
        }
        // poco: borda de pedra, agua escura no fundo, telhadinho em pe em 4 colunas, corrente com lanterna
        for (int a = -1; a <= 1; a++) {
            for (int bb = -1; bb <= 1; bb++) {
                if (a == 0 && bb == 0) {
                    o.set(0, -1, 0, b(Blocks.AIR));
                    o.set(0, -2, 0, b(Blocks.AIR));
                    o.set(0, -3, 0, b(Blocks.WATER));
                    continue;
                }
                o.set(a, -1, bb, b(Blocks.STONE_BRICKS));
                o.forma(a, 0, bb, b(Blocks.STONE_BRICK_WALL));
            }
        }
        for (int[] c : new int[][] {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            o.forma(c[0], 1, c[1], b(Blocks.DARK_OAK_FENCE));
            o.forma(c[0], 2, c[1], b(Blocks.DARK_OAK_FENCE));
        }
        for (int a = -2; a <= 2; a++) {
            o.set(a, 3, -2, o.escada(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false));
            o.set(a, 3, 2, o.escada(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false));
        }
        for (int bb = -1; bb <= 1; bb++) {
            o.set(-2, 3, bb, o.escada(Blocks.DARK_OAK_STAIRS, Direction.EAST, false));
            o.set(2, 3, bb, o.escada(Blocks.DARK_OAK_STAIRS, Direction.WEST, false));
            for (int a = -1; a <= 1; a++) {
                o.set(a, 3, bb, b(Blocks.DARK_OAK_SLAB));
            }
        }
        o.set(0, 2, 0, b(Blocks.CHAIN));
        o.set(0, 1, 0, b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true));
        // bancos virados para o poco
        for (Direction lado : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            if (r.nextInt(4) == 0) {
                continue;
            }
            int da = lado == Direction.EAST ? 4 : lado == Direction.WEST ? -4 : 0;
            int db = lado == Direction.SOUTH ? 4 : lado == Direction.NORTH ? -4 : 0;
            Direction olhar = lado.getOpposite();
            for (int i = -1; i <= 1; i++) {
                int a = da + (db != 0 ? i : 0);
                int bb = db + (da != 0 ? i : 0);
                o.set(a, 0, bb, o.escada(Blocks.SPRUCE_STAIRS, olhar, false));
            }
        }
        // postes de luz nos cantos
        for (int[] c : new int[][] {{-raio + 1, -raio + 1}, {raio - 1, raio - 1}}) {
            for (int y = 0; y <= 3; y++) {
                o.forma(c[0], y, c[1], b(Blocks.POLISHED_BLACKSTONE_WALL));
            }
            o.set(c[0], 4, c[1], b(Blocks.LANTERN));
        }
        // armadilha: duas lajotas falsas perto do poco
        BlockState falsa = CsmpRegistro.PEDRA_FALSA.get().defaultBlockState();
        int lado = r.nextBoolean() ? 1 : -1;
        o.buracoFalso(3 * lado, 0, falsa);
        o.buracoFalso(3 * lado, 1, falsa);
        o.ajustarFormas();
    }

    // ------------------------------------------------------------------ ruina

    /** O que sobrou de uma casa: paredes quebradas em alturas diferentes, sem telhado, entulho e uma estante caida. */
    private static void ruina(Obra o) {
        Random r = o.r;
        Paleta p = PALETAS[r.nextInt(PALETAS.length)];
        int hw = 3 + r.nextInt(2);
        int hd = 3 + r.nextInt(2);
        o.limpar(hw + 2, hd + 2, 10);
        for (int a = -hw; a <= hw; a++) {
            for (int bb = -hd; bb <= hd; bb++) {
                if (r.nextInt(4) != 0) {
                    o.set(a, -1, bb, b(Blocks.DARK_OAK_PLANKS));
                }
                if (Math.abs(a) != hw && Math.abs(bb) != hd) {
                    if (r.nextInt(14) == 0) {
                        o.set(a, 0, bb, r.nextBoolean() ? b(Blocks.COBBLESTONE) : b(Blocks.COBBLED_DEEPSLATE_SLAB));
                    }
                    continue;
                }
                int altura = r.nextInt(5) == 0 ? 0 : 1 + r.nextInt(4);
                for (int y = 0; y < altura; y++) {
                    o.set(a, y, bb, r.nextInt(4) == 0 ? p.rachada() : p.parede());
                }
            }
        }
        o.set(1, 0, 1, b(Blocks.BOOKSHELF));
        o.set(-1, 0, 0, o.escada(Blocks.DARK_OAK_STAIRS, Direction.EAST, false));
        o.set(0, 0, hd - 1, b(Blocks.COBWEB));
        if (r.nextBoolean()) {
            BlockState falso = CsmpRegistro.ASSOALHO_FALSO.get().defaultBlockState();
            o.buracoFalso(-1, -1, falso);
        }
        o.ajustarFormas();
    }

    // ------------------------------------------------------------------ mobiliario de rua

    /** Poste de luz da rua: haste de 4 blocos, braco para o lado da rua e lanterna pendurada (alguns quebrados). */
    static void poste(WorldGenLevel n, BlockPos base, Direction paraRua, Random r) {
        int altura = r.nextInt(6) == 0 ? 2 : 4;
        for (int y = 0; y < altura; y++) {
            n.setBlock(base.above(y), b(Blocks.POLISHED_BLACKSTONE_WALL), 2);
        }
        if (altura < 4) {
            return; // poste quebrado ao meio
        }
        n.setBlock(base.above(4), b(Blocks.POLISHED_BLACKSTONE), 2);
        BlockPos braco = base.above(4).relative(paraRua);
        n.setBlock(braco, b(Blocks.POLISHED_BLACKSTONE_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP), 2);
        if (r.nextInt(5) != 0) {
            n.setBlock(braco.below(), b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 2);
        }
    }
}
