package com.mensagemrapida;

import static org.robolectric.Shadows.shadowOf;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, manifest = Config.NONE, qualifiers = "w360dp-h740dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class FotosTest {
    static final String PASTA = System.getProperty("fotos.dir");
    MainActivity a;

    void idle() { shadowOf(Looper.getMainLooper()).idle(); }

    void foto(String nome) throws Exception {
        idle();
        View v = a.getWindow().getDecorView();
        v.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2220, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, 1080, 2220);
        Bitmap b = Bitmap.createBitmap(1080, 2220, Bitmap.Config.ARGB_8888);
        v.draw(new Canvas(b));
        FileOutputStream out = new FileOutputStream(new File(PASTA, nome + ".png"));
        b.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.close();
    }

    @Test
    public void fotografar() throws Exception {
        Field f = Dados.class.getDeclaredField("instancia");
        f.setAccessible(true);
        f.set(null, null);
        a = Robolectric.buildActivity(MainActivity.class).setup().get();
        foto("01-chegou");
        Dados d = Dados.de(a);
        d.criarConta("Kamilah", "k@exemplo.com", "1234");
        String[][] cs = {{"Fulano", Redes.WHATSAPP}, {"Ciclano", Redes.INSTAGRAM}, {"Beltrano", Redes.FACEBOOK}};
        for (String[] x : cs) {
            Dados.Contato c = new Dados.Contato();
            c.id = Dados.novoId(); c.nome = x[0]; c.meio = x[1]; c.telefone = "11912345678"; c.usuario = "@" + x[0];
            c.criado = System.currentTimeMillis();
            d.contatos.add(c);
        }
        Dados.Mensagem m = new Dados.Mensagem();
        m.id = Dados.novoId(); m.nome = "Olá!"; m.texto = "Olá! Cheguei em casa, tá tudo bem 😊";
        m.criado = System.currentTimeMillis();
        for (Dados.Contato c : d.contatos) m.destinatarios.add(c.id);
        d.mensagens.add(m);
        Dados.Mensagem m2 = new Dados.Mensagem();
        m2.id = Dados.novoId(); m2.nome = "Bom dia"; m2.texto = "Bom dia, {nome}! ☀️"; m2.criado = System.currentTimeMillis();
        m2.destinatarios.add(d.contatos.get(0).id);
        d.mensagens.add(m2);
        d.ativa = m.id;
        d.registrarEnvio("Olá!", 3);
        d.salvar();
        a.irPara(new TelaEntrar()); foto("02-entrar");
        a.irPara(new TelaInicio()); foto("03-inicio");
        a.findViewById(android.R.id.content);
        a.irPara(new TelaSalvarNovo()); foto("05-salvar-novo");
        a.irPara(new TelaFormContato(null)); foto("06-novo-contato");
        a.irPara(new TelaFormMidia()); foto("07-nova-midia");
        a.irPara(new TelaFormMensagem(null)); foto("08-nova-mensagem");
        a.irPara(new TelaMensagens()); foto("09-mensagens");
        a.irPara(new TelaInfoMensagem(m)); foto("14-info");
        a.irPara(new TelaContatos()); foto("16-contatos");
        a.irPara(new TelaContas()); foto("18-contas");
        a.irPara(new TelaConfig()); foto("19-config");
        a.irPara(new TelaHistorico()); foto("20-historico");
        a.irPara(new TelaInicio());
        a.envio.enviar(m);
        idle();
        a.envio.itens.get(0).estado = Envio.ENVIADO;
        a.desenhar();
        foto("21-envio");
        // Gaveta
        a.irPara(new TelaInicio());
        java.util.List<View> vs = new java.util.ArrayList<View>();
        FumacaTest.coletar(a.getWindow().getDecorView(), vs);
        for (View v : vs) if (v instanceof android.widget.TextView && "☰".contentEquals(((android.widget.TextView) v).getText())) v.performClick();
        foto("04-gaveta");
        // Tema escuro
        d.escuro = true;
        d.salvar();
        a = Robolectric.buildActivity(MainActivity.class).setup().get();
        foto("22-escuro");
    }
}
