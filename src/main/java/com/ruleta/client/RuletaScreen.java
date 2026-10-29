package com.ruleta.client;

import com.ruleta.Catalog;
import com.ruleta.Payloads;
import com.ruleta.RuletaColor;
import com.ruleta.RuletaEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

/** Menu del admin: elige color, opcion (o aleatorio), mensaje opcional y lanza. */
public class RuletaScreen extends Screen {
    private int colorSel = -1;   // -1 = aleatorio
    private int optionSel = -1;  // -1 = aleatorio
    private ButtonWidget colorBtn, optionBtn;
    private TextFieldWidget customField;

    public RuletaScreen() {
        super(Text.literal("Ruleta - Panel de admin"));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 - 55;

        colorBtn = addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
            colorSel++;
            if (colorSel >= RuletaColor.values().length) colorSel = -1;
            optionSel = -1;
            refresh();
        }).dimensions(cx - 110, y, 220, 20).build());

        optionBtn = addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
            if (colorSel < 0) return;
            int size = Catalog.forColor(RuletaColor.byId(colorSel)).size();
            optionSel++;
            if (optionSel >= size) optionSel = -1;
            refresh();
        }).dimensions(cx - 110, y + 26, 220, 20).build());

        customField = new TextFieldWidget(textRenderer, cx - 110, y + 62, 220, 20, Text.literal("Mensaje"));
        customField.setMaxLength(200);
        customField.setPlaceholder(Text.literal("Mensaje propio (solo color amarillo)"));
        addDrawableChild(customField);

        addDrawableChild(ButtonWidget.builder(Text.literal("LANZAR RULETA"), b -> {
            ClientPlayNetworking.send(new Payloads.Request(colorSel, optionSel, customField.getText()));
            close();
        }).dimensions(cx - 110, y + 94, 220, 24).build());

        refresh();
    }

    private void refresh() {
        if (colorSel < 0) {
            colorBtn.setMessage(Text.literal("Color: ALEATORIO"));
            optionBtn.setMessage(Text.literal("Opcion: ALEATORIA"));
        } else {
            RuletaColor c = RuletaColor.byId(colorSel);
            colorBtn.setMessage(Text.literal("Color: " + c.label + " (" + c.category + ")").formatted(c.fmt));
            List<RuletaEntry> list = Catalog.forColor(c);
            optionBtn.setMessage(Text.literal(optionSel < 0 || optionSel >= list.size()
                    ? "Opcion: ALEATORIA" : "Opcion: " + list.get(optionSel).name()));
        }
    }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        super.render(g, mx, my, delta); // dibuja el fondo (en 1.21 no se llama renderBackground aparte)
        g.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 80, 0xFFFFFF);
    }
}
