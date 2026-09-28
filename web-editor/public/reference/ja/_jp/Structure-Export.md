# 構造のエクスポート

## 日本語

`/exportStructure` は StructureLib のプレビュー、または読み込まれた GuideNH GameScene のブロックから PNG スクリーンショットを出力します。このコマンドはクライアント側で動作します。`structureLib` サブコマンドは StructureLib が読み込まれている場合だけ使用できます。

エクスポートは正投影で描画されます。構造やシーンの大きさによって出力画像のサイズは変わりますが、設定したブロック倍率は一定です。既定値は 1 ブロックあたり 128 ピクセルです。

### コマンド形式

```text
/exportStructure structureLib [controller] [options...]
/exportStructure structureLib @file.json
/exportStructure structureLib --config file.json

/exportStructure gameScene [options...]
/exportStructure gameScene @file.json
/exportStructure gameScene --config file.json
```

`structureLib` の `controller` は `modid:name` または `modid:name:meta` です。省略すると、すべての StructureLib コントローラーの検出を試みます。

`gameScene` では位置指定のセレクターを使いません。現在読み込まれているガイドからコンパイルされたすべての GameScene を出力します。

### 共通パラメーター

| パラメーター | 説明 |
| --- | --- |
| `--out <dir>` | 出力ディレクトリ。既定値は `screenshots/structurelib/<timestamp>/` または `screenshots/gameScene/<timestamp>/` |
| `--pixelsPerBlock <int>` | ワールドの 1 ブロックあたりのピクセル密度。既定値 `128` |
| `--scale <float>` | `pixelsPerBlock` に掛ける倍率 |
| `--layers <expr\|each\|all>` | レイヤーの表示。既定値 `all` |
| `--view <preset>` | カメラプリセット。StructureLib の既定値 `isometric-south-east`、GameScene はシーン固有のカメラ |
| `--yaw <deg>` / `--pitch <deg>` / `--roll <deg>` | カメラの微調整。GameScene で指定すると、シーンカメラからフィット済み出力カメラへ切り替えます |
| `--rotateX <deg>` / `--rotateY <deg>` / `--rotateZ <deg>` | カメラ上書きの互換エイリアス |
| `--background transparent\|dark\|#RRGGBB\|#AARRGGBB` | PNG の背景。既定値 `transparent` |
| `--maxPixels <long>` | 1 画像の最大ピクセル数。既定値 `655360000`、`-1` で無制限 |
| `--batchSize <int>` | 完了した結果がこの数に達するたびに `manifest.json` を書き込みます。既定値 `16` |
| `--force` | 256 枚を超えるスクリーンショットを許可します |
| `--dry-run` | PNG を書かずに計画とマニフェストだけ作成します |
| `--config <file>` / `@file.json` | JSON 設定を読み込みます |

### StructureLib パラメーター

| パラメーター | 説明 |
| --- | --- |
| `--tier <expr>` | マスターティア値 |
| `--channel <name=expr>` | 名前付き StructureLib チャンネル値。複数指定できます |
| `--facing <list>` | 一括向き出力の向き |
| `--rotation <list>` | 一括向き出力の回転 |
| `--flip <list>` | 一括向き出力の反転 |
| `--orientation <facing:rotation:flip,...>` | 明示的な向きの組み合わせ |
| `--gt-active-controller` | GregTech 専用。可能なら稼働中の機械テクスチャーでコントローラーを描画します |
| `--gt-place-hatches` | GregTech 専用。Hatch 専用プレビュー位置に通常の GT Hatch チャンネル配置を使います。省略時はフォールバック外装です |

### GameScene パラメーター

| パラメーター | 説明 |
| --- | --- |
| `--show-annotations` | ワールド内とオーバーレイのシーン注釈を描画します。既定値 `false` |
| `--show-grid` | シーン床のグリッドを描画します。既定値 `false` |

GameScene は既定で各シーンのカメラを使います。フィット済み出力カメラを使う場合は `--view`、`--yaw`、`--pitch`、`--roll`、`--rotateX`、`--rotateY`、`--rotateZ` のいずれかを指定します。

### 数値フィルター

数値フィルターは `--tier`、`--channel`、`--layers` で使います。

```text
0
0-12
0-12,!5
!0,1
```

`0` は 1 つの値、`0-12` は両端を含む範囲に一致します。`!` は値を除外し、カンマでトークンを組み合わせます。

### レイヤー

`--layers all` は構造またはシーン全体を 1 枚に出力します。

`--layers 0-12,!5` は一致するレイヤーだけを表示した画像を 1 枚出力します。

`--layers each` は実際の Y レイヤーごとに 1 枚出力します。レイヤーフィルターを使う描画では、隣接レイヤーを隠しても露出面が欠けないようブロック面を強制描画します。

### StructureLib のティアとチャンネル

`--tier` と `--channel` を省略すると、コントローラーを調べ、利用できる統合ティアごとに 1 枚出力します。同じティア値をマスターと検出された全チャンネルに適用し、コントローラーと向きの組み合わせごとに最大 100 枚まで出力します。

`--tier` だけを指定すると、各ティア値がすべてのチャンネルに適用され、各チャンネルの範囲に収められます。`--channel` を 1 つ以上指定した場合は、明示した値を使います。複数のティアとチャンネルは直積として組み合わせられます。

```text
/exportStructure structureLib gregtech:gt.blockmachines:1234 --tier 1,2 --channel coil=1-4 --channel casing=1,2
```

### GregTech オプション

Hatch を置ける位置は既定では通常のフォールバック外装として描画されます。Muffler 位置など強制された Hatch 要素は要求された Hatch として描画されます。

プレビュー専用のスクリーンショットで通常の StructureLib Hatch チャンネル配置を使うには `--gt-place-hatches` を指定します。`atLeast(InputHatch, OutputHatch, InputBus, OutputBus, Maintenance, Energy).buildAndChain(casing)` のような要素は必要な Hatch プレビューを配置し、フォールバック外装ではなくテクスチャーを更新できます。

コントローラーを稼働中のテクスチャーで描画するには `--gt-active-controller` を使います。機械チェックに失敗しても、プレビュー状態の同期は行われ、エクスポート失敗にはしません。

### 向き

一括形式:

```text
--facing north,south --rotation normal,clockwise --flip none
```

明示形式:

```text
--orientation north:normal:none,south:clockwise:none
```

両方の形式を同時に使えます。StructureLib の整列制限で拒否される組み合わせはスキップされます。向きを指定しない場合はコントローラーの既定値を使います。

### ビュー

対応プリセット:

```text
isometric-north-east
isometric-south-east
isometric-north-west
top
```

```text
--view isometric-south-east --yaw 315 --pitch 30 --roll 0
```

### JSON 設定

StructureLib の例:

```json
{
  "controller": "gregtech:gt.blockmachines:1234",
  "out": "screenshots/structurelib/demo",
  "pixelsPerBlock": 128,
  "scale": 1.0,
  "tier": "1-4",
  "channels": { "coil": "1-4", "casing": "1,2" },
  "layers": "0-12,!5",
  "orientation": "north:normal:none,south:clockwise:none",
  "view": "isometric-south-east",
  "background": "transparent",
  "maxPixels": 655360000,
  "batchSize": 16,
  "gtActiveController": false,
  "gtPlaceHatches": false,
  "force": false,
  "dryRun": false
}
```

GameScene の例:

```json
{
  "out": "screenshots/gameScene/demo",
  "pixelsPerBlock": 128,
  "scale": 1.0,
  "layers": "all",
  "background": "transparent",
  "maxPixels": 655360000,
  "batchSize": 16,
  "showAnnotations": false,
  "showGrid": false,
  "force": false,
  "dryRun": false
}
```

```text
/exportStructure structureLib @my_structurelib_export.json
/exportStructure gameScene @my_gamescene_export.json
```

相対設定パスは現在の作業ディレクトリと `config/guidenh/structure_exports/` から検索されます。

### 出力とマニフェスト

各出力ディレクトリには PNG ファイルと `manifest.json` が作成されます。StructureLib の画像名はコントローラーの表示名で始まり、ティア、チャンネル、レイヤー、向き、ビューのサフィックスが付きます。GameScene の画像名にはガイド ID、ページ ID、シーン番号、レイヤー、カメラモード、注釈/グリッドのサフィックスが含まれます。Windows のファイル名規則に合わせて名前を無害化し、マニフェストには出力パス、画像サイズ、選択したバリアント、警告、エラーを記録します。

### パフォーマンス

`--force` がない場合、256 枚を超える計画は拒否されます。既定では 1 枚あたり `655360000` ピクセルを超えられません。巨大な構造で容量が足りない場合は `--pixelsPerBlock` または `--scale` を下げる、`--layers` で切り出す、`--maxPixels` を上げる、または `--maxPixels -1` で制限を無効化してください。GPU テクスチャー上限を超える大きな画像は、タイル分割したフレームバッファーで描画されます。
