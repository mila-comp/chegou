package com.mensagemrapida;

/** Idioma do app: cada texto é escrito em português e inglês no próprio lugar onde é usado. */
final class L {
    static boolean ingles;

    static String t(String pt, String en) {
        return ingles ? en : pt;
    }

    private L() { }
}
