# syntaxe Completion


Cette documentation décrit la syntaxe et les fonctions d’exécution de GuideNH. Le code, les balises, les chemins, les identifiants et les valeurs d’attributs restent inchangés.

GuideNH's page éditeur completes MDX balises, leurs attributs, attribut valeurs, markdown syntaxe, code
fence names et frontmatter clés. None of que est hardcoded: everything Le éditeur offers comes depuis a
registry que votre mod peut Ajoutez vers.

There sont deux ways dans, et you normally want les deux.

## 1. balise names come depuis votre balise compilateur

Le éditeur lists Le balise names of chaque enregistré `TagCompiler` et `SceneElementTagCompiler`. Si votre
mod déjà registers Un balise compilateur, son balises sont offered dans Le éditeur avec non extra fonctionnent:

A `TagCompiler` peut être enregistré pour chaque guide à travers
`GuideNhIntegrationRegistry.registerTagCompilerProvider(...)`, et a `SceneElementTagCompiler` à travers
`registerSceneElementTagCompilerProvider(...)`; les deux peut aussi être ajouté vers un guide avec
`GuideBuilder.extension(...)`. Un provider est lire lorsque Un guide est built, so enregistrer it pendant votre mod est
chargement rather que pendant someone est reading.

```java
public class MyModMachineTagCompiler implements TagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Set.of("MyMachine");
    }

    @Override
    public void compileBlockContext(PageCompiler compiler, LytBlockContainer parent, MdxJsxFlowElement el) {
        // ...
    }
}
```

enregistrer it Le même way you déjà do:

```java
GuideNhIntegrationRegistry.global().registerTagCompilerProvider(compilers -> compilers.add(new MyModMachineTagCompiler()));
```

ou pour Un unique guide:

```java
Guide.builder(id).extension(TagCompiler.EXTENSION_POINT, new MyModMachineTagCompiler()).build();
```

balises Un compilateur accepts but page authors jamais écrire - analyseur sortie comme `p`, `h1` ou `em` - sont ne
offered. Le library masque son propre avec `SyntaxSink.hiddenTags(...)`.

## 2. Un syntaxe contributor adds Le rest

Un balise compilateur fait ne say qui attributs it reads ou what those attributs take. que est what a
`SyntaxContributor` est pour. It aussi declares markdown snippets, code fence names et frontmatter clés.

```java
public class MyModSyntaxContributor implements SyntaxContributor {

    @Override
    public String namespace() {
        return "mymod";
    }

    @Override
    public void contribute(SyntaxSink sink) {
        // Attributes. The value kind decides which value source answers completion.
        sink.attributes(
            "MyMachine",
            AttributeSyntax.of("id", SyntaxValueKind.STRING),
            AttributeSyntax.of("machine", SyntaxValueKind.of("MYMOD_MACHINE")),  // your own kind
            AttributeSyntax.of("tier", SyntaxValueKind.INT),
            AttributeSyntax.of("formed", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("mode", SyntaxValueKind.ENUM, "input", "output", "both"));

        // Tags that wrap content are completed as <Name></Name> with the caret inside.
        sink.containerTags("MyPanel");
        sink.children("MyPanel", "MyMachine", "Tooltip");

        // Values resolved from your own registry at completion time.
        sink.valueSource(new MachineIdValueSource());

        // Extra markdown and fence names.
        sink.markdown(MarkdownSnippet.block("::", "Mod note", ":: note\n", 3));
        sink.fenceLanguages("mymod-diagram");

        // Frontmatter keys, with fixed values or a value kind.
        sink.frontmatterKeys("mymod_machine");
        sink.frontmatterKind("mymod_machine", SyntaxValueKind.of("MYMOD_MACHINE"));
    }
}
```

enregistrer it globally, ou per guide:

```java
GuideNhIntegrationRegistry.global().registerSyntaxContributor(new MyModSyntaxContributor());
Guide.builder(id).extension(SyntaxContributor.EXTENSION_POINT, new MyModSyntaxContributor()).build();
```

Globally enregistré contributors sont applied premier, so Un guide's propre contributor peut remplacer eux.

Le built-dans syntaxe que ships avec Le mod est un ordinary contributor (`BuiltinSyntaxContributor`)
enregistré as Un par défaut extension. `GuideBuilder.disableDefaultExtensions()` donc turns Le whole
built-dans syntaxe off along avec Le par défaut balise compilers.

## 3. valeur kinds et valeur sources

Un attribut's `SyntaxValueKind` decides how Le éditeur completes et quotes son valeur:

| Kind | Completion | Written as |
| --- | --- | --- |
| `STRING` | free texte (ou Le valeurs declared suivant vers Le attribut) | `"..."` |
| `INT`, `FLOAT` | numeric presets pour well known attribut names | bare |
| `BOOLEAN` | `true`, `false` | bare |
| `ENUM` | Le constants of Le enum passed vers `AttributeSyntax.of`, ou Le declared valeurs | `"..."` |
| `COLOR` | `#rrggbb` plus Le symbolic colour names | `"..."` |
| `ITEM_ID`, `BLOCK_ID` | élément et bloc registry names, avec icons | `"..."` |
| `ORE_DICT`, `ENTITY_ID`, `KEY_BIND`, `COMMAND` | Le correspondant game registry | `"..."` |
| `PAGE_PATH`, `FILE_PATH` | guide page ids, et files dans Le guide assets | `"..."` |
| `MOD_ID` | mod ids taken depuis Le élément registry namespaces | `"..."` |
| `EXPRESSION`, `DOMAIN`, `FORMAT_PATTERN` | courant expressions, intervals et patterns | `"..."` |
| `VECTOR3` | free texte | bare |
| `SNBT` | free texte | `{...}` |
| `QUEST_UUID` | free texte; quest ids sont ne discoverable yet | `"..."` |

Reusing Un built-dans kind signifie you do ne enregistrer anything: marking Un attribut as `ITEM_ID` est enough
vers get élément completion pour it.

vers complet depuis votre propre registry, declare Un kind et Un source. Un kind est just Un nom, so you peut
declare it where you like:

```java
public final class MyModValueKinds {

    public static final SyntaxValueKind MACHINE = SyntaxValueKind.of("MYMOD_MACHINE");
}
```

```java
public class MachineIdValueSource implements SyntaxValueSource {

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(MyModValueKinds.MACHINE);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        List<SyntaxSuggestion> results = new ArrayList<>();
        for (MyMachine machine : MyMachineRegistry.all()) {
            if (results.size() >= limit) {
                break;
            }
            if (machine.id()
                .startsWith(request.partialText())) {
                results.add(SyntaxSuggestion.withValue(machine.id(), machine.id(), machine.displayName()));
            }
        }
        return results;
    }
}
```

`request` carries Le typed texte, Le enclosing balise et attribut, et Le frontmatter clé, so un
source peut serve several attributs.

## 4. Live données

Un source que needs données depuis Le open guide ou depuis Le document implements
`SyntaxEnvironmentAware`. `prepare` est called once per completion tick, avant quelconque request est answered:

```java
public class MyMachineValueSource implements SyntaxValueSource, SyntaxEnvironmentAware {

    @Override
    public void prepare(SyntaxEnvironment environment) {
        // environment.guide(), environment.pagePaths(), environment.documentText()
    }
}
```

Conservez `prepare` cheap: it runs on chaque éditeur tick. Cache derived lists et seulement rebuild eux lorsque Le
entrée actually changed - que est what Le built-dans `PagePathValueSource` et `AnchorValueSource` do.

## 5. Insert templates

Completing Un balise nom writes `<Name />`, ou Un paired balise avec Le caret dans Le container. Un balise que
needs attributs ou contenu vers être useful declares Le texte it devrait complet as à la placer:

```java
sink.insertTemplates(
    InsertTemplate.caretAfter("MyMachine", "<MyMachine id=\"\" tier=\"1\" />", "id=\""),
    InsertTemplate.caretAfter("MyBlock", "<MyBlock id=\"\">\n  \n</MyBlock>", "\n  "));
```

`caretAfter` puts Le caret droite après Le premier occurrence of Le marker it est given, so Un template peut
say where typing continues sans counting characters. `InsertTemplate.of(tag, text)` writes Le texte
avec Le caret at son fin. Le template replaces Le typed balise nom, so Le author accepts it et carries
on dans Le form que was written.

Le mod itself declares templates pour Le balises whose useful form est plus que Le balise nom, pour Exemple
`<BlockStat item="" count="1" />`, `<InputAnnotation pos="0.5 1.5 0.5" inputType="lmb" />` et Un graphique
avec son premier série.

## 6. Slots: syntaxe of votre propre

Everything au-dessus de describes syntaxe ceci mod déjà parses. pour syntaxe of votre propre - Un directive dans a
bloc, Un small langue dans Un code fence, Un champ you compilateur yourself - enregistrer a `SyntaxSlot`:

```java
public class MyModSlot implements SyntaxSlot {

    @Override
    public String namespace() {
        return "mymod";
    }

    @Override
    public SyntaxSlotMatch match(String text, int cursorIndex, GuideSyntaxModel model) {
        // Own the value of one attribute of your own tag, while its quote is still open.
        String attribute = "ports=\"";
        int attributeStart = text.lastIndexOf(attribute, cursorIndex);
        if (attributeStart < 0) {
            return null;
        }
        int from = attributeStart + attribute.length();
        int closingQuote = text.indexOf('"', from);
        if (closingQuote >= 0 && cursorIndex > closingQuote) {
            return null;
        }
        return new SyntaxSlotMatch(
            from,
            cursorIndex,
            text.substring(from, cursorIndex),
            MyPortRegistry.suggestions(),
            suggestion -> SyntaxReplacement.cursorAtEnd(suggestion.value()));
    }
}
```

enregistrer it globally ou per guide, exactement like Un contributor:

```java
GuideNhIntegrationRegistry.global().registerSyntaxSlot(new MyModSlot());
Guide.builder(id).extension(SyntaxSlot.EXTENSION_POINT, new MyModSlot()).build();
```

What Un slot controls:

- `match` decides Le range Un accepted valeur replaces, Le texte déjà typed dans it et Le valeurs
  Le éditeur offers. retourner `null` lorsque Le caret est ne dans votre syntaxe. Le texte you report as typed
  a vers être what Le document really holds dans que range: Le éditeur re-checks it avant writing, et a
  normalized form of it would drop Le commit.
- Le `SyntaxValueWriter` of Le correspondre writes Un accepted valeur: Le texte que replaces Le range, where
  Le caret lands et what est sélectionné. Leaving it `null` replaces Le range avec Le valeur itself. A
  writer que throws ou answers nothing est reported dans Le log et Le éditeur writes nothing.
- `selection` optionally declares Le range Un double clic dans votre syntaxe selects.

Slots sont asked avant Le éditeur's propre resolvers, et Le premier slot que correspond owns Le caret even
lorsque it offers non valeurs, so ceci mod jamais guesses dans texte another mod claims.

## 7. What Le éditeur déjà completes

- MDX balises, scoped vers Le enclosing container, avec container balises completing as `<Name></Name>`.
- attribut names, qui expand vers `name=""`, `name={}` ou `name={true}` according vers Le valeur kind.
- attribut valeurs, routed by valeur kind, including recipes, élément et bloc ids, et mod ids.
- Markdown syntaxe: headings, lists, task lists, quotes, alerts, tableaux, code blocs, thematic breaks,
  math blocs, et inline emphasis, code, liens et images. chaque snippet places Le caret where you
  continue typing.
- Code fence names, derived depuis Le code bloc langue registry plus Le GuideNH spécifique fences.
- YAML frontmatter clés, et valeurs pour Le clés que declare Un valeur kind.
- Insert templates, so Un balise whose useful form est plus que son nom est written complet.

Candidates sont ranked so Le popup pre-selects Le closest correspondre: Un exact prefix premier, alors Un correspondre on
Le identifier après Le namespace, alors Un correspondre anywhere dans Le nom.

## 8. What you peut extend, et what you ne peut pas yet

Everything on ceci page est un registry, et Un enregistré plugin est seulement lire lorsque Un guide est built ou Un page
compilé, so enregistrer pendant mod chargement.

| You want vers | utiliser |
| --- | --- |
| Ajoutez Un balise, Un scène element, Un attribut, Un valeur kind et son valeurs | `TagCompiler`, `SceneElementTagCompiler`, `SyntaxContributor`, `SyntaxValueSource` |
| complet syntaxe of votre propre (votre propre slot, votre propre writer, double-clic range) | `SyntaxSlot` |
| faire Un balise complet as Un whole form | `sink.insertTemplates(...)` (aussi offered dans Le éditeur's insert menu) |
| Ajoutez markdown snippets, fence names, frontmatter clés et valeurs | `SyntaxContributor` |
| Decide what Le corps of votre propre fence signifie | `CodeFenceRenderer` (declare Le nom avec `sink.fenceLanguages(...)`; remplacer `renderSiteFence` so Le site affiche it too) |
| Ajoutez Un symbolic colour nom | `SymbolicColorResolver` (per guide) ou `registerSymbolicColorResolver` |
| Ajoutez Un page index | `GuideBuilder.index(...)` / `GuideBuilderIntegrationHook` |
| Export votre balises vers Le site | `GuideSiteTagRenderer` (Voir [Mod Compatibility](Mod-Compatibility)) |
| Ajoutez Un scène éditeur toolbar button ou menu élément | `SceneEditorToolbarRegistry` / `SceneEditorMenuRegistry` |
| Ajoutez Un ressource vers Le exported site | `ExportableResourceProvider` on votre node |
| Report Un problem avec votre propre syntaxe vers Le reader | `LytErrorSink.appendError(compiler, text, element)`, qui est Le `parent` votre balise compilateur est given |
| Ajoutez Un button ou menu entrée vers Le guide éditeur | `GuideNhIntegrationRegistry.registerEditorAction(GuideEditorActionContribution.insert(...))` ou `.wrap(...)`, ou `GuideEditorActionContribution.Provider` pour un guide |

Deliberately closed, avec what it costs you:

- Le guide éditeur's built-dans toolbar est un fixe Définissez. Un contributed *action* (Voir
  `GuideEditorActionContribution`) gets son propre toolbar button et menu entrée après Le built-ins, so what
  ne peut pas être ajouté est un modifier vers Un built-dans action, ne Un new un.
- Diagnostics sont reported at compilateur time à travers `LytErrorSink`: Le erreur bloc est appended vers Le
  document, so Le reader sees it dans Le book et on Le exported site. There est non separate "warn
  pendant typing" hook dans Le éditeur.
- Le site export renders son propre balises avec Un built-dans chain. Un enregistré renderer est asked premier, pour
  chaque MDX element et pour votre fence names, so votre balises et fences export; Le built-dans chain itself est
  ne Un Définissez of enregistré renderers yet.
- scène annotations of votre propre rendent dans Le book but sont ne serialized dans Le exported site's viewer.
- Un scène element of votre propre, enregistré as a `SceneElementTagCompiler`, renders dans Le book et reaches Le
  site seulement Si son element est un `SceneAnnotation`: que est Le un shape Le exporter collects. Un element of
  quelconque autre shape, comme un que dessine géométrie of son propre, est gauche out of Le export pendant Le rest of Le
  scène est exported around it. Exporting such Un element needs Un annotation que carries it, ou Un site
  renderer pour Le balise que produces Le markup.

Conventions que matter pour Un plugin:

- Give votre `SyntaxValueKind` Un mod-namespaced id (`"MYMOD_MACHINE"`). Kinds route vers valeur sources by que
  id alone; Le quoting hint belongs vers Le attribut que declares Le valeur.
- retourner what Le document really holds depuis `SyntaxSlot.match`: Le éditeur re-checks Le texte avant it
  writes, et Un normalized form drops Le commit.
- Un plugin que throws est reported avec son namespace et skipped, so Un failure costs votre propre
  contributions rather que Le éditeur.
