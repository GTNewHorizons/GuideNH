# Localização

Esta documentação descreve a sintaxe e os recursos de execução do GuideNH. Código, tags, caminhos, IDs e valores de atributos permanecem intactos.


GuideNH suporta localizado guia pages e localizado guia assets.

## Estrutura de pastas

localização em execução é pasta-based:

```text
config/guidenh/DefaultGuide/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

O repository Exemplo pacote de recursos keeps seu documented outer `assets/` layer:

```text
wiki/resourcepack/assets/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

idioma folders são recognized somente em O underscored form. simples folders como `en_us/` e `zh_cn/` são não longer treated as Localização roots.

## Ordem de busca de páginas

Para cada requested ID da página, GuideNH tries:

1. `_<current language>/<page>`
2. `_<default language>/<page>` Se O idioma atual página é ausente
3. `<page>` sem Uma pasta de idioma

guia pages somente fall back para O guia's `defaultLanguage`. Auto-discovered recurso-pack guias ainda Padrão que valor para `en_us`, so another idioma é não promoted em Uma idioma de reserva just because it exists.

## página Lang chave substitui

guia pages pode também substituir seus completo fonte Markdown de a `.lang` chave, but somente quando O physical página arquivo
já exists. O arquivo remains O existence gate e O reserva origem.

- GuideNH primeiro resolves O página arquivo com O normal idioma reserva ordem
- depois de Uma arquivo tem been found, GuideNH looks para Uma página-localizado `.lang` valor para O requested idioma
- Se que chave exists e é non-vazio, seu completo valor torna-se O página fonte Markdown antes de parsing
- Se O chave é ausente ou vazio, GuideNH falls back para O resolvido arquivo conteúdo

quando Uma non-vazio `.lang` página valor é usado, GuideNH merges ausente frontmatter campos de O resolvido physical
página antes de parsing. localizado frontmatter sempre wins para campos it explicitly defines, so translated
`navigation.title` valores stay localizado, enquanto newer structural campos como `navigation.recommend`,
`navigation.priority`, categories, item links, authorship metadata, ou página zoom pode ser inherited de O
reserva `.md` arquivo. Este keeps older completo-página translations de accidentally dropping home-página recommendations
quando O base página metadata é atualizado.

O chave format é:

```text
guidenh.page.<namespace>.<folder>.<page path without .md>
```

Exemplo:

```text
assets/guidenh/guidenh/_en_us/charts.md
-> guidenh.page.guidenh.guidenh.charts
```

caminho separators become `.` em O chave. Literal dots dentro de Uma caminho segment são escaped so Elas do não collide com
segment separators:

```text
foo.bar.md -> foo_x2e_bar
```

outro non-alphanumeric characters são escaped com O mesmo `_x<hex>_` pattern.

dentro de O `.lang` valor, literal `\n` e `\r` sequences são converted para real linha endings antes de Markdown parsing,
so O tradução valor pode conter Uma completo página including frontmatter, headings, lists, e MDX tags.

Authoring Regras:

- escrever O página as um physical `.lang` linha para que chave
- usar literal `\n` dentro de O valor quando you want Uma Markdown linha break
- do não insert real linha breaks em O `.lang` valor itself, because Forge reads `.lang` files linha by linha
- do não escrever `\\n` a menos que you intentionally want O final fonte Markdown para conter O literal characters `\n`

GuideNH não faz synthesize pages de `.lang` alone. Uma real página arquivo deve ainda exist.

## chave Length

GuideNH não faz impose Um extra character limit on Estes página chaves. On Minecraft 1.7.10 / Forge, O backing idioma
dados é effectively Uma texto-propriedade map, so O practical limits são normal memory usage e maintainability rather
que Uma dedicated hard cap. Shorter página paths ainda fazer chaves easier para author e review.

## Recomendações de autoria

- Defina `defaultLanguage` deliberately quando you want Uma non-English idioma de reserva para Uma guia
- Adicione Uma shared idioma-neutral página somente quando cross-idioma reserva é actually intended
- translate pages primeiro, então translate assets somente quando texto é embedded em O recurso
- avoid idioma-específico recurso filenames quando Uma rooted shared recurso would do

## Ordem de busca de recursos

guia assets usar Uma slightly richer reserva ordem:

1. `_<current language>/<path>`
2. Se O idioma atual é não O guia idioma padrão, `_<default language>/<path>`
3. `<path>`

Este makes it possible para localize imagens ou texture-like assets quando needed.

## Pesquisa e idioma

pesquisa documents store ambos O bruto Minecraft idioma e O analyzer idioma usado para Lucene. Se O atual Minecraft idioma é não mapped para Uma known analyzer, pesquisa falls back para English tokenization.

## Ignore tradução Config

GuideNH não faz expose Uma global "ignore translations" switch. Se you want Uma guia para fall back para Uma non-English idioma, Defina que guia's `defaultLanguage` explicitly em code.

## Exemplo

```text
config/guidenh/DefaultGuide/guidenh/guidenh/_en_us/index.md
config/guidenh/DefaultGuide/guidenh/guidenh/_zh_cn/index.md
config/guidenh/DefaultGuide/guidenh/guidenh/_en_us/test1.png
config/guidenh/DefaultGuide/guidenh/guidenh/_zh_cn/test1.png
```

Repository Exemplo pack equivalent:

```text
wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/index.md
wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/test1.png
```

## Páginas relacionadas

- [Guide Page Format](Guide-Page-Format)
- [Images And Assets](Images-And-Assets)
