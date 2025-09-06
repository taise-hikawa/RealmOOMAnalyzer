# MyApplication - OOM検証プロジェクト

## プロジェクト概要
Realm Java使用時のOutOfMemoryError (OOM) 問題を検証・再現するためのテストアプリケーション。
実際のプロダクトで発生したOOMクラッシュを再現し、解決策を検証する。

## 元のクラッシュ情報
```
Fatal Exception: java.lang.OutOfMemoryError: Failed to allocate a 40 byte allocation with 1302880 free bytes and 1272KB until OOM, target footprint 201326592, growth limit 201326592; giving up on allocation because <1% of heap free after GC.
at kotlin.collections.CollectionsKt___CollectionsKt.toList(_Collections.kt:1316)
at jp.co.spacely.phototask.domain.model.Task.<init>(Task.kt:180)
```

## 現在の実装状況

### データモデル
- **ImageTitlesStorage**: 主キーなし、title/category/portalCategoryフィールド
- **TaskStorage**: ImageTitlesStorageのRealmListを持つ、idフィールド  
- **ImageTitlesStorageWithKey**: 主キーあり（正規化データ用）
- **TaskStorageWithKey**: 正規化データ用

### ドメインモデル  
- **TaskImageTitles**: データクラス
- **Task**: Realmオブジェクトからドメインオブジェクトへの変換（OOM発生箇所）

### テスト機能
1. **重複データ作成**: 1,000個のTask × 50個のImageTitles = 50,000個の重複オブジェクト
2. **正規化データ作成**: 1個のImageTitlesをすべてのTaskで共有
3. **Realm監視開始**: RealmChangeListenerでデータ変更を監視
4. **Task一覧取得**: 20回連続でタスク取得（OOM再現）
5. **メモリ使用量表示**: ヒープ使用状況の詳細表示

### UI構成
- MainActivity: 5つのテストボタン + ログ表示
- MemoryTestViewModel: テスト実行とメモリ監視

## 検証済み結果

### OOMクラッシュ再現成功
- **データ規模**: 50,000個のImageTitlesStorage作成
- **クラッシュタイミング**: Task一覧取得の連続実行時
- **メモリ状況**: Minor page fault 150,000回以上、システムメモリ不足

### ログ例
```
faults: 173139 minor 2063 major
some avg10=7.48 avg60=2.16 (memory pressure)
kswapd0: 12% kernel (swap daemon active)
System available memory: 107 MB
```

## 技術詳細

### Realm設定
- UIスレッドでの書き込み許可: `allowWritesOnUiThread(true)`
- アプリ起動時にデータ全削除実装済み
- スキーマバージョン1、マイグレーション時にRealm削除

### メモリ監視
- ActivityManagerとRuntimeを使用したヒープ監視
- Page fault数の詳細ログ
- GC前後のメモリ使用量比較
- システムメモリ使用量表示

## 次のステップ

### 解決策実装予定
1. **データ正規化**: 主キー使用による重複排除
2. **遅延変換**: lazy評価による必要時変換
3. **ページング**: 大量データの分割処理
4. **メモリ監視**: 早期警告システム

### 優先度
1. データ正規化（最重要 - メモリ使用量90%削減）
2. 遅延変換（OOM直接対策）
3. ページング（UX改善）
4. メモリ監視（早期検知）

## 開発環境
- Android Studio
- Kotlin
- Realm Java 10.18.0
- Jetpack Compose
- Target SDK: 36
- Min SDK: 26

## コマンド
- ビルド: `./gradlew assembleDebug`
- テスト実行: 1→4の順でボタン実行
- ログ確認: Logcat で "MemoryTest" タグをフィルタ