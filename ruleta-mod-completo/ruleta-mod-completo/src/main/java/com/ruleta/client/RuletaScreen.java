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

/** Compacto panel de administracion para seleccionar, anunciar y detener eventos. */
public class RuletaScreen extends Screen {
    private int colorSel = 0;
    private int optionSel = -1;
    private ButtonWidget colorBtn, optionBtn;
    private TextFieldWidget titleField, messageField;

    public RuletaScreen() { super(Text.literal("Panel de la ruleta")); }

    @Override
    protected void init() {
        int panelW = Math.min(360, width - 24);
        int left = (width - panelW) / 2;
        int y = Math.max(16, (height - 282) / 2);
        int innerW = panelW - 32;

        colorBtn = addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
            saveSelected();
            colorSel = (colorSel + 1) % RuletaColor.values().length;
            optionSel = -1;
            loadSelected();
            refresh();
        }).dimensions(left + 16, y + 34, innerW, 20).build());
        optionBtn = addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
            List<RuletaEntry> list = Catalog.forColor(RuletaColor.byId(colorSel));
            optionSel = (optionSel + 1) % (list.size() + 1);
            if (optionSel == list.size()) optionSel = -1;
            refresh();
        }).dimensions(left + 16, y + 59, innerW, 20).build());

        titleField = new TextFieldWidget(textRenderer, left + 16, y + 89, innerW, 20, Text.literal("Titulo"));
        titleField.setMaxLength(64);
        titleField.setPlaceholder(Text.literal("Titulo editable del evento"));
        addDrawableChild(titleField);
        messageField = new TextFieldWidget(textRenderer, left + 16, y + 114, innerW, 20, Text.literal("Mensaje"));
        messageField.setMaxLength(240);
        messageField.setPlaceholder(Text.literal("Mensaje del evento"));
        addDrawableChild(messageField);
        loadSelected();

        int gap = 6;
        int halfW = (innerW - gap) / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("LANZAR"), b -> {
            saveSelected();
            ClientPlayNetworking.send(new Payloads.Request(colorSel, optionSel, titleField.getText(), messageField.getText()));
            close();
        }).dimensions(left + 16, y + 145, halfW, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("AVISAR"), b -> {
            saveSelected();
            ClientPlayNetworking.send(new Payloads.Control(1, colorSel, titleField.getText(), messageField.getText()));
        }).dimensions(left + 16 + halfW + gap, y + 145, halfW, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("DETENER EVENTOS"), b -> {
            ClientPlayNetworking.send(new Payloads.Control(0, colorSel, "", ""));
        }).dimensions(left + 16, y + 172, innerW, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("BOTÓN MORTAL · 100.000 DAÑO"), b -> {
            ClientPlayNetworking.send(new Payloads.Control(2, RuletaColor.ROJA.ordinal(), "", ""));
        }).dimensions(left + 16, y + 199, innerW, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("RULETA DE OBJETOS  ·  10 TÓTEMS + CORAZÓN"), b -> {
            ClientPlayNetworking.send(new Payloads.Control(3, RuletaColor.AMARILLA.ordinal(), "", ""));
        }).dimensions(left + 16, y + 226, innerW, 22).build());
        refresh();
    }

    private void refresh() {
        RuletaColor color = RuletaColor.byId(colorSel);
        colorBtn.setMessage(Text.literal("COLOR  •  " + color.label + " — " + color.category).formatted(color.fmt));
        List<RuletaEntry> list = Catalog.forColor(color);
        optionBtn.setMessage(Text.literal(optionSel < 0 || optionSel >= list.size()
                ? "EVENTO  •  Aleatorio" : "EVENTO  •  " + list.get(optionSel).name()).formatted(color.fmt));
    }

    private void loadSelected() {
        RuletaColor color = RuletaColor.byId(colorSel);
        titleField.setText(RuletaClientConfig.title(color));
        messageField.setText(RuletaClientConfig.message(color));
    }

    private void saveSelected() {
        if (titleField != null && messageField != null)
            RuletaClientConfig.save(RuletaColor.byId(colorSel), titleField.getText(), messageField.getText());
    }

    @Override
    public void renderBackground(DrawContext g, int mx, int my, float delta) {
        super.renderBackground(g, mx, my, delta);
        int panelW = Math.min(360, width - 24);
        int left = (width - panelW) / 2;
        int y = Math.max(16, (height - 282) / 2);
        g.fill(left - 3, y - 3, left + panelW + 3, y + 264, 0xFF070A10);
        g.fill(left - 1, y - 1, left + panelW + 1, y + 262, 0xFF40536C);
        g.fill(left, y, left + panelW, y + 260, 0xF5101723);
        g.fill(left + 1, y + 1, left + panelW - 1, y + 3, 0xFFE4B85C);
        g.fill(left + 1, y + 4, left + panelW - 1, y + 5, 0xFF5BC0EB);
        g.fill(left + 12, y + 31, left + panelW - 12, y + 32, 0xFF344258);
        g.fill(left + 12, y + 84, left + panelW - 12, y + 85, 0xFF344258);
    }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        int panelW = Math.min(360, width - 24);
        int left = (width - panelW) / 2;
        int y = Math.max(16, (height - 282) / 2);
        int color = RuletaColor.byId(colorSel).argb;
        g.fill(left + 16, y + 34, left + 20, y + 54, color);
        g.fill(left + 16, y + 59, left + 20, y + 79, color);
        g.drawCenteredTextWithShadow(textRenderer, Text.literal("RULETA  /  PANEL DE CONTROL").styled(s -> s.withBold(true)), width / 2, y + 12, 0xFFFFF4D8);
        g.drawCenteredTextWithShadow(textRenderer, Text.literal("Configura y anuncia el siguiente evento"), width / 2, y + 23, 0xFFB7C5D8);
        g.drawCenteredTextWithShadow(textRenderer, Text.literal("TÍTULO Y MENSAJE DEL EVENTO"), width / 2, y + 137, 0xFF9FB2C8);
        g.drawCenteredTextWithShadow(textRenderer, Text.literal("Ajustes guardados por color"), width / 2, y + 256, 0xFF75869B);
    }
}
