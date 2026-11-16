package com.mensagemrapida;

import android.content.Context;
import android.graphics.Color;

/** As redes sociais/canais que o app conhece. */
final class Redes {
    static final String WHATSAPP = "WhatsApp";
    static final String INSTAGRAM = "Instagram";
    static final String TELEGRAM = "Telegram";
    static final String X = "X";
    static final String FACEBOOK = "Facebook";
    static final String SNAPCHAT = "Snapchat";
    static final String SMS = "SMS";

    static final String[] TODAS = {WHATSAPP, INSTAGRAM, TELEGRAM, X, FACEBOOK, SNAPCHAT, SMS};

    static int cor(String rede) {
        if (WHATSAPP.equals(rede)) return Color.rgb(37, 168, 76);
        if (INSTAGRAM.equals(rede)) return Color.rgb(168, 85, 247);
        if (TELEGRAM.equals(rede)) return Color.rgb(42, 132, 210);
        if (SNAPCHAT.equals(rede)) return Color.rgb(234, 128, 32);
        if (FACEBOOK.equals(rede)) return Color.rgb(24, 100, 230);
        return Ui.texto; // X e SMS: cor do texto
    }

    /** Pacote do app oficial (para abrir direto nele), ou null para SMS. */
    static String pacote(String rede) {
        if (WHATSAPP.equals(rede)) return "com.whatsapp";
        if (INSTAGRAM.equals(rede)) return "com.instagram.android";
        if (TELEGRAM.equals(rede)) return "org.telegram.messenger";
        if (X.equals(rede)) return "com.twitter.android";
        if (FACEBOOK.equals(rede)) return "com.facebook.orca";
        if (SNAPCHAT.equals(rede)) return "com.snapchat.android";
        return null;
    }

    /** WhatsApp e SMS chegam pelo telefone; as demais, pelo nome de usuário (Telegram: Chat ID). */
    static boolean usaTelefone(String rede) {
        return WHATSAPP.equals(rede) || SMS.equals(rede);
    }

    /** Envia sem nenhum toque extra? (o resto abre o app com tudo pronto) */
    static boolean automatica(String rede, boolean temMidia) {
        return TELEGRAM.equals(rede) || (SMS.equals(rede) && !temMidia);
    }

    static boolean instalado(Context c, String rede) {
        String pkg = pacote(rede);
        if (pkg == null) return true;
        try {
            c.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (Exception e) {
            if (WHATSAPP.equals(rede)) {
                try {
                    c.getPackageManager().getPackageInfo("com.whatsapp.w4b", 0);
                    return true;
                } catch (Exception e2) {
                    return false;
                }
            }
            return false;
        }
    }

    static String rotuloUsuario(String rede) {
        if (TELEGRAM.equals(rede)) return L.t("Chat ID do Telegram", "Telegram chat ID");
        return L.t("Usuário no ", "Username on ") + rede;
    }

    private Redes() { }
}
