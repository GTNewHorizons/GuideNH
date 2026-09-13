package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.command.ICommand;
import net.minecraftforge.client.ClientCommandHandler;

import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

public class CommandValueSource implements SyntaxValueSource {

    private final NameSnapshot snapshot = new NameSnapshot(CommandValueSource::registeredNames);

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(SyntaxValueKind.COMMAND);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        String partial = request.partialText();
        String bare = partial != null && partial.startsWith("/") ? partial.substring(1) : partial;
        List<String> names = snapshot.match(bare, limit);
        List<SyntaxSuggestion> results = new ArrayList<>(names.size());
        for (String name : names) {
            results.add(SyntaxSuggestion.of("/" + name));
        }
        return results;
    }

    private static List<String> registeredNames() {
        List<String> names = new ArrayList<>();
        for (Object command : ClientCommandHandler.instance.getCommands()
            .values()) {
            if (command instanceof ICommand registered) {
                String name = registered.getCommandName();
                if (name != null && !name.isEmpty()) {
                    names.add(name);
                }
            }
        }
        return names;
    }
}
