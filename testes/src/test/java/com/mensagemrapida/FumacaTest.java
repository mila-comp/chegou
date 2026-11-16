package com.mensagemrapida;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowSmsManager;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, manifest = Config.NONE)
public class FumacaTest {

    ActivityController<MainActivity> ctl;

    @Before
    public void zerar() throws Exception {
        Field f = Dados.class.getDeclaredField("instancia");
        f.setAccessible(true);
        f.set(null, null);
        L.ingles = false;
    }

    MainActivity a() { return ctl.get(); }

    void idle() { shadowOf(Looper.getMainLooper()).idle(); }

    View raiz() { return a().getWindow().getDecorView(); }

    static void coletar(View v, List<View> out) {
        out.add(v);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) coletar(g.getChildAt(i), out);
        }
    }

    TextView acharTexto(View raiz, String s) {
        List<View> vs = new ArrayList<View>();
        coletar(raiz, vs);
        for (View v : vs) {
            if (v instanceof TextView && !(v instanceof EditText) && v.isShown()
                    && ((TextView) v).getText().toString().trim().equals(s)) return (TextView) v;
        }
        for (View v : vs) {
            if (v instanceof TextView && !(v instanceof EditText) && v.isShown()
                    && ((TextView) v).getText().toString().contains(s)) return (TextView) v;
        }
        return null;
    }

    boolean tem(String s) { return acharTexto(raiz(), s) != null; }

    void clicar(View raiz, String s) {
        View v = acharTexto(raiz, s);
        assertNotNull("não achei: " + s + "\nTela:\n" + despejar(raiz), v);
        while (!v.isClickable()) v = (View) v.getParent();
        v.performClick();
        idle();
    }

    void clicar(String s) { clicar(raiz(), s); }

    void segurar(String s) {
        View v = acharTexto(raiz(), s);
        assertNotNull(s, v);
        while (!v.isLongClickable()) v = (View) v.getParent();
        v.performLongClick();
        idle();
    }

    EditText campo(String dica) {
        List<View> vs = new ArrayList<View>();
        coletar(raiz(), vs);
        for (View v : vs) {
            if (v instanceof EditText && v.isShown() && String.valueOf(((EditText) v).getHint()).contains(dica)) {
                return (EditText) v;
            }
        }
        fail("campo não achado: " + dica + "\n" + despejar(raiz()));
        return null;
    }

    List<EditText> campos() {
        List<View> vs = new ArrayList<View>();
        coletar(raiz(), vs);
        List<EditText> r = new ArrayList<EditText>();
        for (View v : vs) if (v instanceof EditText && v.isShown()) r.add((EditText) v);
        return r;
    }

    static String despejar(View raiz) {
        List<View> vs = new ArrayList<View>();
        coletar(raiz, vs);
        StringBuilder sb = new StringBuilder();
        for (View v : vs) {
            if (v instanceof TextView && v.isShown()) sb.append("  [").append(((TextView) v).getText()).append("]\n");
        }
        return sb.toString();
    }

    AlertDialog dialogo() {
        AlertDialog d = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        assertNotNull("sem diálogo", d);
        return d;
    }

    void botaoDialogo(int qual) {
        dialogo().getButton(qual).performClick();
        idle();
    }

    void itemDialogo(int i) {
        shadowOf(dialogo()).clickOnItem(i);
        idle();
    }

    void tela(String nome) {
        System.out.println("=== " + nome + " ===\n" + despejar(raiz()));
    }

    /** Toca em ENVIAR e confirma na folha que sobe. */
    void enviarEConfirmar(int pessoas) {
        clicar("ENVIAR");
        clicar("Enviar para " + pessoas + (pessoas == 1 ? " pessoa" : " pessoas"));
    }

    void abrirApp() {
        ctl = Robolectric.buildActivity(MainActivity.class).setup();
        idle();
    }

    void criarConta() {
        clicar("Criar conta");
        campo("Nome de usuário").setText("Kamilah");
        campo("E-mail").setText("k@exemplo.com");
        campo("Senha (mínimo").setText("1234");
        campo("Repita").setText("1234");
        clicar("Criar");
    }

    @Test
    public void fluxoCompleto() {
        abrirApp();
        tela("1 boas-vindas");
        assertTrue(tem("Chegou"));
        clicar("Entrar");
        assertTrue("deve avisar que não há conta", tem("Ainda não há conta"));

        criarConta();
        tela("3 início vazio");
        assertTrue(tem("Nenhuma mensagem ainda"));

        // ENVIAR sem mensagem leva à criação
        clicar("ENVIAR");
        tela("8 nova mensagem");
        campo("Digite aqui").setText("Olá! Cheguei em casa.");
        campo("Bom dia").setText("Cheguei");
        assertTrue(tem("21/1000"));
        clicar("Salvar");
        tela("14 info (após criar)");
        assertTrue(tem("Encaminhar para"));
        assertTrue(tem("Ninguém ainda"));

        // Novo contato WhatsApp
        clicar("Novo contato");
        tela("6 novo contato");
        campos().get(0).setText("Fulano");
        campos().get(1).setText("11 91234-5678");
        clicar("SELECIONA");
        itemDialogo(0); // WhatsApp
        clicar("Salvar");
        // Contato SMS
        clicar("Novo contato");
        campos().get(0).setText("Ciclano");
        campos().get(1).setText("11 99999-0000");
        clicar("SELECIONA");
        itemDialogo(6); // SMS
        clicar("Salvar");
        // Contato Instagram (precisa de usuário)
        clicar("Novo contato");
        campos().get(0).setText("Beltrano");
        clicar("SELECIONA");
        itemDialogo(1); // Instagram
        tela("6 contato instagram");
        clicar("Salvar");
        assertTrue("deve exigir usuário", tem("Preencha"));
        campo("@usuario").setText("@beltrano");
        clicar("Salvar");
        tela("14 info com contatos");

        // Marca os três
        List<View> vs = new ArrayList<View>();
        coletar(raiz(), vs);
        int n = 0;
        for (View v : vs) {
            if (v instanceof CheckBox && !((CheckBox) v).getText().toString().contains("barra de notificações")) {
                ((CheckBox) v).setChecked(true);
                n++;
            }
        }
        assertEquals(3, n);
        clicar("Salvar");
        tela("3 início com mensagem");
        assertTrue(tem("Cheguei"));
        assertTrue(tem("Fulano, Ciclano, Beltrano"));

        // ENVIAR: sobe a confirmação; cancelar não envia nada
        clicar("ENVIAR");
        tela("confirmação");
        assertTrue(tem("Enviar “Cheguei”?"));
        clicar("Cancelar");
        assertNull(shadowOf(a()).getNextStartedActivity());
        // Confirmando: pede permissão de SMS
        enviarEConfirmar(3);
        shadowOf(a()).grantPermissions(Manifest.permission.SEND_SMS);
        a().onRequestPermissionsResult(1, new String[]{Manifest.permission.SEND_SMS},
                new int[]{android.content.pm.PackageManager.PERMISSION_GRANTED});
        idle();
        tela("envio em andamento");
        // O simulador não tem telefonia: o SMS do Ciclano aparece como falha na lista.
        assertTrue(tem("SMS falhou"));
        // A primeira conversa abre sozinha, sem mais um toque
        Intent w = shadowOf(a()).getNextStartedActivity();
        assertNotNull("WhatsApp deveria abrir sozinho", w);
        System.out.println("WhatsApp: " + w.getData() + " pkg=" + w.getPackage());
        assertTrue(w.getData().toString().contains("phone=5511912345678"));
        assertTrue(w.getData().toString().contains("Ol%C3%A1"));

        // Volta do WhatsApp: a tela já oferece a próxima conversa
        ctl.pause().resume();
        idle();
        tela("envio após 1ª conversa");
        assertTrue(tem("✓ aberta"));
        clicar("Abrir conversa com Beltrano");
        Intent ig = shadowOf(a()).getNextStartedActivity();
        System.out.println("Instagram: " + ig.getData() + " pkg=" + ig.getPackage());
        assertEquals("https://ig.me/m/beltrano", ig.getData().toString());
        ClipboardManager cb = (ClipboardManager) a().getSystemService(Context.CLIPBOARD_SERVICE);
        assertEquals("Olá! Cheguei em casa.", cb.getPrimaryClip().getItemAt(0).getText().toString());
        ctl.pause().resume();
        idle();
        tela("envio concluído");
        assertTrue(tem("Envio concluído"));
        clicar("Concluir");
        assertTrue("histórico no início", tem("Último envio"));
        clicar("Último envio");
        tela("histórico");
        assertTrue(tem("3 pessoas"));
        clicar("‹");

        // Gaveta → Mensagens salvas
        clicar("☰");
        tela("4 gaveta");
        clicar("💬  Mensagens salvas");
        tela("9 mensagens salvas");
        assertTrue(tem("Criado:"));
        segurar("Cheguei");
        PopupWindow pw = ShadowApplication.getInstance().getLatestPopupWindow();
        assertNotNull("popup de opções", pw);
        clicar(pw.getContentView(), "Editar");
        tela("15 editar");
        assertTrue(tem("Editar mensagem"));
        campo("Digite aqui").setText("Oi, cheguei!");
        clicar("Salvar");
        assertTrue(tem("Mensagens salvas"));

        // Nova mídia
        clicar("☰");
        clicar("➕  Salvar novo");
        tela("5 salvar novo");
        clicar("Nova mídia");
        tela("7 nova mídia");
        clicar("Escolher arquivo");
        Uri foto = Uri.parse("content://teste/foto.jpg");
        ShadowContentResolver scr = shadowOf(a().getContentResolver());
        scr.registerInputStream(foto, new ByteArrayInputStream(new byte[2048]));
        Intent pedido = shadowOf(a()).getNextStartedActivityForResult().intent;
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, pedido.getAction());
        shadowOf(a()).receiveResult(pedido, Activity.RESULT_OK, new Intent().setData(foto));
        idle();
        tela("7 com arquivo");
        campos().get(0).setText("Foto da casa");
        clicar("Salvar");
        assertEquals(1, Dados.de(a()).midias.size());
        assertEquals(2048, Dados.de(a()).midias.get(0).tamanho);

        // Anexa mídia pela Info e envia de novo (WhatsApp com mídia usa ACTION_SEND)
        clicar("☰");
        clicar("💬  Mensagens salvas");
        clicar("Cheguei");
        tela("14 info");
        clicar("Nenhuma");
        itemDialogo(1);
        clicar("Salvar");
        clicar("☰");
        clicar("🏠  Início");
        assertTrue(tem("📎 Foto da casa"));
        enviarEConfirmar(3);
        Intent comMidia = shadowOf(a()).getNextStartedActivity();
        System.out.println("Com mídia: " + comMidia + " extras=" + comMidia.getExtras());
        assertEquals(Intent.ACTION_SEND, comMidia.getAction());
        assertEquals("5511912345678@s.whatsapp.net", comMidia.getStringExtra("jid"));
        // SMS com mídia vira manual: aparece na lista para abrir
        ctl.pause().resume();
        idle();
        assertTrue(tem("Abrir conversa com Ciclano"));

        // Contatos salvos: info, excluir
        clicar("☰");
        clicar("👥  Contatos salvos");
        tela("16 contatos");
        clicar("Fulano");
        tela("info contato");
        assertTrue(tem("Recebe as mensagens"));
        clicar("Enviar mensagem só para Fulano");
        itemDialogo(0);
        clicar("Enviar para 1 pessoa");
        tela("envio só para Fulano");
        assertTrue(tem("1 de 1"));
        assertNotNull(shadowOf(a()).getNextStartedActivity());
        clicar("‹");
        clicar("Voltar");
        segurar("Beltrano");
        pw = ShadowApplication.getInstance().getLatestPopupWindow();
        clicar(pw.getContentView(), "Excluir");
        assertTrue(String.valueOf(shadowOf(dialogo()).getMessage()).contains("certeza"));
        botaoDialogo(AlertDialog.BUTTON_POSITIVE);
        assertTrue(tem("Excluído com sucesso"));
        assertFalse(tem("Beltrano"));
        assertEquals(2, Dados.de(a()).mensagens.get(0).destinatarios.size());

        // Contas vinculadas
        clicar("☰");
        clicar("🔗  Contas vinculadas");
        tela("18 contas");
        assertTrue(tem("Snapchat"));
        clicar("Telegram");
        dialogo();

        // Telegram sem bot configurado: avisa
        // Configurações
        clicar("☰");
        clicar("⚙️  Configuração");
        tela("19 config");
        assertTrue(tem("Kamilah"));
        clicar("Armazenamento e dados");
        tela("armazenamento");
        assertTrue(tem("Foto da casa"));
        clicar("VOLTAR".toLowerCase().equals("x") ? "" : "Foto da casa");
        botaoDialogo(AlertDialog.BUTTON_POSITIVE);
        assertEquals(0, Dados.de(a()).midias.size());
        ctl.get().onBackPressed();
        idle();

        // Acessibilidade
        clicar("Acessibilidade");
        botaoDialogo(AlertDialog.BUTTON_POSITIVE);
        idle();
        // Tema escuro (recria a Activity e volta às configurações)
        clicar("Aparência");
        itemDialogo(1);
        idle();
        tela("config escura");
        assertTrue("após trocar tema deve continuar nas configurações", tem("Excluir conta"));
        assertTrue(Dados.de(a()).escuro);

        // Idioma inglês
        clicar("Idioma");
        itemDialogo(1);
        idle();
        tela("config em inglês");
        assertTrue(tem("Delete account"));
        clicar("☰");
        assertTrue(tem("Saved messages"));
        clicar("🏠  Home");
        assertTrue(tem("SEND"));

        // Sair e entrar de novo
        clicar("☰");
        clicar("⚙️  Settings");
        clicar("Account");
        itemDialogo(4);
        tela("após sair");
        clicar("Sign in");
        campo("E-mail").setText("k@exemplo.com");
        campo("Password").setText("errada");
        clicar("Sign in");
        assertTrue(tem("Wrong e-mail or password"));
        campo("Password").setText("1234");
        clicar("Sign in");
        assertTrue(tem("Last message"));

        // Excluir conta
        clicar("☰");
        clicar("⚙️  Settings");
        clicar("Delete account");
        botaoDialogo(AlertDialog.BUTTON_POSITIVE);
        tela("após excluir");
        assertTrue(tem("Create account"));
        assertEquals(0, Dados.de(a()).contatos.size());
    }

    @Test
    public void widgetEnviaAoAbrir() {
        abrirApp();
        criarConta();
        Dados d = Dados.de(a());
        Dados.Contato c = new Dados.Contato();
        c.id = "c1"; c.nome = "Ana Maria"; c.telefone = "21988887777"; c.meio = Redes.WHATSAPP;
        d.contatos.add(c);
        Dados.Mensagem m = new Dados.Mensagem();
        m.id = "m1"; m.nome = "Oi"; m.texto = "Oi {nome}, tudo bem?"; m.destinatarios.add("c1");
        d.mensagens.add(m);
        d.salvar();
        Intent i = new Intent(a(), MainActivity.class).setAction(WidgetEnviar.ACAO_ENVIAR);
        ctl.newIntent(i);
        idle();
        tela("widget: confirmação");
        assertTrue("widget pede confirmação", tem("Enviar “Oi”?"));
        assertNull(shadowOf(a()).getNextStartedActivity());
        clicar("Enviar para 1 pessoa");
        Intent w = shadowOf(a()).getNextStartedActivity();
        assertNotNull("confirmou: abre a conversa sozinho", w);
        assertTrue(w.getData().toString().contains("phone=5521988887777"));
        assertTrue("{nome} vira o primeiro nome", w.getData().toString().contains("text=Oi+Ana%2C"));

        // Voltando da conversa, tudo certo: volta sozinho ao Início
        ctl.pause().resume();
        idle();
        assertTrue(tem("Tudo enviado ✓"));
        assertTrue(tem("ENVIAR"));

        // Com a confirmação desligada, um toque já abre a conversa
        d.confirmar = false;
        clicar("ENVIAR");
        assertNotNull(shadowOf(a()).getNextStartedActivity());
    }

    @Test
    public void migraVersaoAntiga() {
        SharedPreferences p = RuntimeEnvironment.getApplication().getSharedPreferences("dados", Context.MODE_PRIVATE);
        p.edit().putString("mensagem", "Mensagem antiga")
                .putString("contatos", "[{\"nome\":\"Zé\",\"canal\":\"SMS\",\"destino\":\"1199\",\"selecionado\":true}]")
                .commit();
        abrirApp();
        Dados d = Dados.de(a());
        assertEquals(1, d.contatos.size());
        assertEquals("1199", d.contatos.get(0).telefone);
        assertEquals("Mensagem antiga", d.mensagens.get(0).texto);
        assertEquals(1, d.mensagens.get(0).destinatarios.size());
        // Sem conta ainda: começa nas boas-vindas
        assertTrue(tem("Criar conta"));
    }

    @Test
    public void telasDeTodasAsTelas() {
        abrirApp();
        criarConta();
        Tela[] telas = {new TelaInicio(), new TelaSalvarNovo(), new TelaFormContato(null), new TelaFormMidia(),
                new TelaFormMensagem(null), new TelaMensagens(), new TelaContatos(), new TelaContas(),
                new TelaConfig(), new TelaArmazenamento(), new TelaEnvio(), new TelaHistorico(), new TelaEntrar(), new TelaBoasVindas(),
                new TelaCriarConta()};
        for (Tela t : telas) {
            a().abrir(t);
            idle();
        }
        System.out.println("todas as telas abriram");
    }

    Dados.Contato contato(Dados d, String nome, String meio) {
        Dados.Contato c = new Dados.Contato();
        c.id = Dados.novoId(); c.nome = nome; c.telefone = "119" + Math.abs(nome.hashCode() % 100000000);
        c.usuario = "@" + nome; c.meio = meio; c.criado = System.currentTimeMillis();
        d.contatos.add(c);
        return c;
    }

    @Test
    public void whatsAppTodosDeUmaVez() {
        abrirApp();
        criarConta();
        Dados d = Dados.de(a());
        Dados.Mensagem m = new Dados.Mensagem();
        m.id = "m1"; m.nome = "Cheguei"; m.texto = "Cheguei em casa!";
        m.destinatarios.add(contato(d, "Ana", Redes.WHATSAPP).id);
        m.destinatarios.add(contato(d, "Bia", Redes.WHATSAPP).id);
        m.destinatarios.add(contato(d, "Caio", Redes.INSTAGRAM).id);
        d.mensagens.add(m);
        d.salvar();
        a().irPara(new TelaInicio());
        idle();
        enviarEConfirmar(3);
        tela("envio com WhatsApp em grupo");
        assertTrue(tem("Ana, Bia"));
        assertTrue(tem("1 de 2"));
        Intent w = shadowOf(a()).getNextStartedActivity();
        assertEquals(Intent.ACTION_SEND, w.getAction());
        assertEquals("com.whatsapp", w.getPackage());
        assertEquals("Cheguei em casa!", w.getStringExtra(Intent.EXTRA_TEXT));
        assertNull("sem destinatário fixo: o WhatsApp mostra a lista", w.getStringExtra("jid"));

        // Com {nome} cada pessoa recebe um texto diferente: volta a ser uma conversa por pessoa
        m.texto = "Oi {nome}, cheguei!";
        a().irPara(new TelaInicio());
        idle();
        enviarEConfirmar(3);
        assertTrue(tem("1 de 3"));
        assertTrue(tem("Abrir conversa com Bia"));

        // Desligando a opção também
        m.texto = "Cheguei em casa!";
        d.whatsJunto = false;
        a().irPara(new TelaInicio());
        idle();
        enviarEConfirmar(3);
        assertTrue(tem("1 de 3"));
    }

    @Test
    public void lembreteNaNotificacao() {
        abrirApp();
        criarConta();
        Dados d = Dados.de(a());
        Dados.Mensagem m = new Dados.Mensagem();
        m.id = "m1"; m.nome = "Cheguei"; m.texto = "Cheguei em casa!";
        m.destinatarios.add(contato(d, "Ana", Redes.WHATSAPP).id);
        m.destinatarios.add(contato(d, "Bia", Redes.WHATSAPP).id);
        d.mensagens.add(m);
        d.salvar();
        android.app.NotificationManager nm = (android.app.NotificationManager)
                a().getSystemService(Context.NOTIFICATION_SERVICE);
        assertEquals("sem lembrete, sem notificação", 0, shadowOf(nm).size());

        // Liga o lembrete pela tela Info
        a().abrir(new TelaInfoMensagem(m));
        idle();
        tela("info com lembrete desligado");
        List<View> vs = new ArrayList<View>();
        coletar(raiz(), vs);
        for (View v : vs) {
            if (v instanceof CheckBox && ((CheckBox) v).getText().toString().contains("barra de notificações")) {
                ((CheckBox) v).setChecked(true);
            }
        }
        idle();
        tela("info com lembrete ligado");
        assertTrue(tem("16:00"));
        assertTrue(tem("19:00"));
        clicar("Salvar");
        assertTrue(m.lembrete);
        assertTrue("início mostra o lembrete", tem("Lembrete das 16:00 às 19:00"));

        // Janela que contém "agora": a notificação aparece, com o botão Enviar
        java.util.Calendar c = java.util.Calendar.getInstance();
        int agora = c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE);
        m.lembreteInicio = (agora + 1439) % 1440;
        m.lembreteFim = (agora + 60) % 1440;
        d.salvar();
        assertEquals(1, shadowOf(nm).size());
        android.app.Notification n = shadowOf(nm).getAllNotifications().get(0);
        assertEquals("Cheguei", n.extras.getString(android.app.Notification.EXTRA_TITLE));
        System.out.println("NOTIFICAÇÃO: " + n.extras.getCharSequence(android.app.Notification.EXTRA_TEXT)
                + " | ação: " + n.actions[0].title);
        assertEquals("Avisar Ana e Bia?", String.valueOf(n.extras.getCharSequence(android.app.Notification.EXTRA_TEXT)));
        assertEquals("Enviar agora", String.valueOf(n.actions[0].title));
        android.app.AlarmManager am = (android.app.AlarmManager) a().getSystemService(Context.ALARM_SERVICE);
        assertNotNull("alarme para o fim/início da janela", shadowOf(am).getNextScheduledAlarm());

        // WhatsApp precisa abrir o app: o botão leva à tela de envio
        assertTrue(shadowOf(n.actions[0].actionIntent).isActivityIntent());
        ctl.newIntent(shadowOf(n.actions[0].actionIntent).getSavedIntent());
        idle();
        tela("confirmação a partir da notificação");
        assertEquals("enquanto não confirma, a notificação continua", 1, shadowOf(nm).size());
        clicar("Enviar para 2 pessoas");
        assertEquals(Intent.ACTION_SEND, shadowOf(a()).getNextStartedActivity().getAction());
        assertEquals("depois de enviar, a notificação some", 0, shadowOf(nm).size());
        d.salvar();
        assertEquals("e não volta no mesmo dia", 0, shadowOf(nm).size());

        // Dispensar (arrastar para o lado) também vale pelo dia
        m.lembreteFeito = 0;
        d.salvar();
        assertEquals(1, shadowOf(nm).size());
        new Lembretes().onReceive(a(), new Intent(Lembretes.ACAO_DISPENSAR).putExtra(Lembretes.EXTRA_MENSAGEM, "m1"));
        nm.cancelAll();
        d.salvar();
        assertEquals(0, shadowOf(nm).size());

        // Fora da janela não aparece
        m.lembreteFeito = 0;
        m.lembreteInicio = (agora + 120) % 1440;
        m.lembreteFim = (agora + 180) % 1440;
        d.salvar();
        assertEquals(0, shadowOf(nm).size());

        // Dia da semana desmarcado não aparece
        m.lembreteInicio = (agora + 1439) % 1440;
        m.lembreteFim = (agora + 60) % 1440;
        m.lembreteDias = 127 & ~(1 << (c.get(java.util.Calendar.DAY_OF_WEEK) - 1));
        d.salvar();
        assertEquals(0, shadowOf(nm).size());

        // Só Telegram com bot configurado: o botão envia em segundo plano, sem abrir o app
        m.lembreteDias = 127;
        m.destinatarios.clear();
        m.destinatarios.add(contato(d, "Davi", Redes.TELEGRAM).id);
        d.telegramToken = "123:abc";
        d.salvar();
        n = shadowOf(nm).getAllNotifications().get(0);
        assertTrue("envio automático não abre o app", shadowOf(n.actions[0].actionIntent).isBroadcastIntent());

        // Tocar em Enviar pede confirmação na própria notificação
        new Lembretes().onReceive(a(), shadowOf(n.actions[0].actionIntent).getSavedIntent());
        n = shadowOf(nm).getAllNotifications().get(0);
        assertEquals(2, n.actions.length);
        assertEquals("Confirmar ✓", String.valueOf(n.actions[0].title));
        assertEquals("Confirmar envio para Davi?", String.valueOf(n.extras.getCharSequence(android.app.Notification.EXTRA_TEXT)));
        // Cancelar volta ao botão Enviar
        new Lembretes().onReceive(a(), shadowOf(n.actions[1].actionIntent).getSavedIntent());
        n = shadowOf(nm).getAllNotifications().get(0);
        assertEquals(1, n.actions.length);
        assertEquals("Enviar agora", String.valueOf(n.actions[0].title));
    }
}
