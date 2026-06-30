package de.anxbiss.shortcuts.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

public final class ShortcutExecutor {
    private ShortcutExecutor() {
    }

    public static void execute(String rawCommand) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        ClientPlayNetworkHandler connection = minecraft.getNetworkHandler();
        if (connection == null || rawCommand == null) {
            return;
        }

        String command = rawCommand.trim();
        if (command.isEmpty()) {
            return;
        }

        String lowerCommand = command.toLowerCase();
        if (lowerCommand.startsWith("chat:")) {
            sendChatMessage(connection, command.substring("chat:".length()));
            return;
        }

        if (lowerCommand.startsWith("c:")) {
            sendChatMessage(connection, command.substring("c:".length()));
            return;
        }

        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        if (!command.isEmpty()) {
            connection.sendChatCommand(command);
        }
    }

    private static void sendChatMessage(ClientPlayNetworkHandler connection, String rawMessage) {
        String message = rawMessage.trim();
        if (!message.isEmpty()) {
            connection.sendChatMessage(message);
        }
    }
}
