Reservoir ユーザーガイド
===
## 概要
Javaで記述されたライブラリで、ヒープの外（メモリ、ファイル）に値を格納する機能を持った、キー／バリュー形式のキャッシュやキューを提供する。

## ヒープ外キャッシュ
`java.util.Map` のようにキー／バリュー形式でデータを保持するデータ構造で、バリューの部分をヒープ外に出力することができる。キーやバリューへの参照はヒープ上で管理するため、ヒープを使用しないわけではない。`net.ihiroky.reservoir.Builder` クラスを用いてキャッシュを組み立てる。キャッシュは `net.ihiroky.reservoir.Cache` によりアクセスする。同期処理方式はインデックスにより決まる。null値は扱えない。キャッシュは、大きく分けてインデックス／キャッシュアクセッサから成る。インデックスはキーの管理を行い、キャッシュアクセッサはバリューとなるデータへのアクセスを行う。インデックス／キャッシュアクセッサの種類に関しては、「[Builder 仕様](#builder_spec)」を参照。  
  
（例）

    Cache<String, String> cache = new Builder().name("sub")
        .indexType(Builder.IndexType.LRU)
        .cacheAccessorType(Builder.CacheAccessorType.BYTE_BUFFER)
        .property("reservoir.ByteBufferCacheAccessor.direct", "true")
        .property("reservoir.ByteBufferCacheAccessor.size", "8192")
        .property("reservoir.ByteBufferCacheAccessor.blockSize", "256")
        .property("reservoir.ByteBufferCacheAccessor.partitions", "8")
        .property("reservoir.ByteBufferCacheAccessor.coder", "net.ihiroky.reservoir.coder.SerializableCoder")
        .createCache();
    cache.put("key", "value");
    cache.put("fizz", "buzz");
    System.out.println("key:" + cache.get("key") + ", fizz:" + cache.get("fizz")); // says 'key:value, fizz:buzz'
    cache.dispose();

## ヒープ外キュー
キューに入れるデータをヒープの外に出すことができる。データへの参照はヒープ上で管理するため、ヒープを使用しないわけではない。`java.util.Queue` インターフェースを実装しており、`java.util.concurrent.ConcurrentLinkedQueue` がベースとなっている。データの保存／読込ロジックは、ヒープ外キャッシュと同様に、キャッシュアクセッサを利用する。キャッシュアクセッサの種類に関しては、「[Builder 仕様](#builder_spec)」を参照。  
  
（例）

    BasicQueue<byte[]> queue = new Builder()
        .indexType(Builder.IndexType.SIMPLE)
        .cacheAccessorType(Builder.CacheAccessorType.BYTE_BUFFER)
        .property("reservoir.ByteBufferCacheAccessor.direct", "true")
        .property("reservoir.ByteBufferCacheAccessor.size", "8192")
        .property("reservoir.ByteBufferCacheAccessor.blockSize", "256")
        .property("reservoir.ByteBufferCacheAccessor.coder", "net.ihiroky.reservoir.coder.ByteArrayCoder")
        .property("reservoir.ByteArrayCoder.compress.enabled", "true")
        .createQueue();
    
    byte[][] b = new byte[3][];
    for (int i=0; i<b.length; i++) {
        b[i] = new byte[512];
        for (int j=0; j<b[i].length; j++) {
            b[i][j] = (byte)((i + '0') % 10);
        }
    }
    queue.offer(b[0]);
    queue.offer(b[1]);
    queue.offer(b[2]);
    try {
        assertThat(queue.poll(), is(b[0]));
        assertThat(queue.poll(), is(b[1]));
        assertThat(queue.poll(), is(b[2]));
        assertThat(queue.poll(), is(nullValue()));
    } finally {
        queue.dispose();
    }

## JMX サポート
キャッシュ、キューともに Platform MBean Server に MBean を登録する。この MBean を jconsole や visualvm 等のツールから参照することでデータ管理データにアクセスできる。この MBean は以下のプロパティ、メソッドを公開している。

### キャッシュ MBean

#### プロパティ
Name : `Builder#name()`または CompoundCacheのコンストラクタで指定した名前(name)。
Size : キャッシュしているキーの数。
CacheAccessorClassName : 使用しているキャッシュアクセッサオブジェクトのクラス名
IndexClassName : 使用しているインデックスオブジェクトのクラス名
StringKeyResolverClassName : 後述の referEntry(), removeEntry(), containsEntry() メソッドで使用する、文字列表現のキーを実際のキーに変換するオブジェトのクラス名

#### メソッド
これらのメソッドは、`Cache#setStringKeyResolver()` によってキーの型に応じた `net.ihiroky.reservoir.StringResolver` が設定されていなければ機能しない。
referEntry(String key) : key で指定されるキーに対するバリューを文字列表現で返す。
removeEntry(String key) : key で指定されるキーとそのバリューを削除する。
boolean containsEntry(String key) : key で指定されるキーがキャッシュに含まれていれば true, 含まれていなければ false を返す。

### キュー MBean

#### プロパティ
Name : `Builder#name()`で指定した名前。  
Size : エンキューされている要素数。

## 複合キャッシュ
`Builder` は、単一の保存領域（メモリ、ファイル）を使用する基本キャッシュを生成する。複数の基本キャッシュを用いて段階的なキャッシュを構成するためとして、複合キャッシュを設けている。
複合キャッシュは二つの基本キャッシュからなり、片方をメインキャッシュ、他方をサブキャッシュとしている。メインキャッシュは通常上限が設定されたキャッシュとし、メインキャッシュのサイズが上限に達した場合、インデックスが指定するキーとそのバリューをサブキャッシュに移動される。メインキャッシュには高速な動作を行うがサイズが限られるキャッシュ（例：ヒープにデータを置くサイズ上限の有るキャッシュ）を用い、サブキャッシュには低速だが上限の制限が緩い。メインキャッシュのサイズに上限が無い場合はサブキャッシュへの移動が起こらないため、複合キャッシュを構成する意味は無い。また、サブキャッシュにサイズ上限がある場合、この上限を超えたキー／バリューは消えてなくなる。複合キャッシュ生成時に指定する 
  
（例）

    Cache<String, String> main = new Build().name("main").size(10)
      ...
      .createCache();
    Cache<String, String> sub = new Builder().name("sub")
      ...
      .createCache();
    Cache<String, String> cache = new CompoundCache("cache name", main, sub);
    cache.put("key", "value");
    cache.put("fizz", "buzz");
    System.out.println("key:" + cache.get("key") + ", fizz:" + cache.get("fizz")); // says 'key:value, fizz:buzz'
    cache.dispose();

## REST サポート
XML/JSON over HTTP による、キャッシュ対する参照／更新／削除操作を行う機能をサポートしている。この機能を利用するためには、HTTPサーバを起動し、公開するキャッシュを登録する必要がある。このサーバは JDK 6 以上に含まれている `com.sun.http` を利用している。
### サーバ
HTTPサーバは `net.ihiroky.reservoir.rest.RestServer` として実装されている。このインスタンスに公開したいキャッシュとキャッシュの値を読み取るための `net.ihiroky.reservoir.coder.XMLCoder` と `net.ihiroky.reservoir.coder.JSONCoder` の実装を登録する。`XMLCoder`, `JSONCoder` はキー／バリューの型に応じた実装を用意する必要がある。そして、`RestSever#start()` を呼び出すとHTTPサーバが起動し、外部へ公開される。以下の例では `http://localhost:32767/reservoir/` がコンテキストルートになる。  
  
（例）  
  
    Cache<String, String> cache = new Builder().name("dic").createCache();
    restServer = new RestServer();
    restServer.addCache(integerCache, MLCoder.STRING_CODER, JSONCoder.STRING_CODER);
    restServer.start("localhost", 32767, "/reservoir");

### API
* **参照**

    HTTP メソッド : GET  
    URL : コンテキストルート/{cache}/{key}  
    * {cache} : キャッシュ名
    * {key} : キー

    指定したキャッシュ名をもつキャッシュの指定したキーに対応するバリューを返す。リクエストヘッダ Accept が text/xml または application/xml の時は XML 形式で、application/json の場合は JSON 形式で応答を返す。また、パラメータとして regex=true を指定すると、key を正規表現として扱う。

    （例）

        $ curl -X GET -H 'Accept: text/xml' http://localhost:32767/reservoir/dic/hoge
        <?xml version="1.0" ?><entries><entry key="hoge">fuga</entry></entries>

        $ curl -X GET -H 'Accept: application/json' http://localhost:32767/reservoir/dic/^h.*e$&regex=true
        [{"k":"home","v":"ie"},{"k":"hoge","v":"fuga"}]

* **更新**

    HTTP メソッド : PUT  
    URL : コンテキストルート/{cache}
    * {cache} : キャッシュ名
    * HTTP ボディに更新内容を記述

    指定したキャッシュ名をもつキャッシュに対して、同封される XML もしくは JSON の内容を更新する。同封するデータのフォーマットは、リクエストヘッダ Content-Type により指定する。XML を利用する場合は Content-Type として text/xml または application/xml を指定し、JSON を利用する場合は Content-Type として application/json を指定する。  
  
   （例）

        $ curl -X PUT -H 'Content-Type: application/xml' \  
            -d '<?xml version="1.0" ?><entries><entry key="hoe">kuwa</entry><entry key="horse">uma</entry></entries>' \  
            http://localhost:32767/reservoir/dic
    
* **削除**

    HTTP メソッド : DELETE
    URL : コンテキストルート/{cache}/{key}
    * {cache} : キャッシュ名
    * {key} : キー

    指定したキャッシュ名をもつキャッシュの指定したキーに対応するバリューを削除し、その値を返す。リクエストヘッダ Accept が text/xml または application/xml の時は XML 形式で、application/json の場合は JSON 形式で応答を返す。また、パラメータとして regex=true を指定すると、key を正規表現として扱う。  
  
    （例）

        $ curl -X DELETE -H 'Accept: text/xml' http://localhost:32767/reservoir/dic/hoge
        <?xml version="1.0" ?><entries><entry key="hoge">fuga</entry></entries>

        $ curl -X DELETE -H 'Accept: application/json' http://localhost:32767/reservoir/dic/^h.*e$&regex=true
        [{"k":"home","v":"ie"},{"k":"hoge","v":"fuga"}]

* メタ情報

    HTTP メソッド : HEAD
    URL : コンテキストルート/{cache}
    * {cache} : キャッシュ名

    指定したキャッシュ名をモツキャッシュのメタ情報をレスポンスヘッダに載せて返す。メタ情報の内容は以下のとおり。

    * X-CACHE-NAME : キャッシュ名
    * X-CACHE-SIZE : キャッシュが保持している要素数
    * X-CACHE-INDEX : キャッシュが使用しているインデックスのクラス名
    * X-CACHE-ACCESSOR : キャッシュが使用しているキャッシュアクセッサのクラス名
    * X-CACHE-KEY-RESOLVER : MBeanのメソッド呼び出し時にキャッシュが使用する `StringResolver` のクラス名
    * X-CACHE-XML-CODER : キャッシュが使用している `XMLCoder` のクラス名
    * X-CACHE-JSON-CODER : キャッシュが使用している `JSONCoder` のクラス名

## メモリアロケータ概要

### ブロック

キャッシュアクセッサには複数のパラメータを与えることができる。このパラメータは使用できる領域に影響を与える。キャッシュアクセッサによる保存領域の割り当てには、一定のバイトを1ブロックとしこれを最小割り当て単位とする固定長アロケータにより割り当てが行われる。基本的には一つのバイト列を、指定されたサイズを持つ固定長ブロックに分けて使用する。以下の模式図では、長さ11のブロックが3つ存在している様子を表している。

    byte array : 012345678901234567890123456789012
    block      : | block A || block B || block C |

今、キャッシュアクセッサがある2つのオブジェクト (obj1, obj2) を一切データが保存されていないような上記バイト列に格納しようとする。格納するオブジェクトが [Coder](#coder_spec) によりバイト列に変換され、そのサイズが10バイト、14バイトであったとき、格納イメージは以下の模式図のようになる。

    byte array : 012345678901234567890123456789012
    block      : | block A || block B || block C |
    allocation : |  obj1  | |    obj2    |

obj1 はブロックサイズ（9バイト）に収まっており block A に格納される。obj2 の割り当ては、空き領域となっているブロックから行うため block A の空き領域をとばし、block B から行われる。obj2 はブロックサイズには収まらないため、次の空き領域である block C にも割り当てが行われる。obj2 により block C も割り当て済みとなり、空きブロックがなくなった（新しくデータを格納すことができない）状態となる。

空き領域もブロック毎に管理されている。ある格納済みオブジェクトが削除されると、オブジェクトが割り当てられていたすべてのブロックが空き領域として扱われ、新しいオブジェクトの割り当てが有れば再利用される。

### ByteBuffer による保存領域制限

キャッシュアクセッサの保存領域として `java.nio.ByteBuffer` を直接的／間接的に使用する場合、`ByteBuffer` の制限から 2G バイト以上の大きさを持った一つの領域を作成することができない。2G バイト以上の領域を確保するときは 2G バイト未満の領域を複数束ねて使用する。キャッシュアクセッサとして [BYTE\_BUFFER](#builder_spec_cacheaccessor) を指定する場合は、保存領域のバイトサイズ、ブロックさイズに加え、2G バイト未満の領域（パーティション）をいくつ作るかも指定する。また、 [MEMORY\_MAPPED\_FILE](#builder_spec_cacheaccessor) を指定する場合は、1ファイルあたりの上限バイトサイズは 2G バイト未満にする必要がある。


<a name="builder_spec"></a>

## Builder 仕様
`net.ihiroky.reservoir.Builder` クラスは、ヒープ外キャッシュ、ヒープ外キューを構築するために必要なパラメータを設定するためのインターフェースとなる。

* 名前 `Builder#name()`

    キャッシュに付与する名前を設定する。JMXにより公開されるMBeanのオブジェクト名や、REST APIによるキャッシュアクセスを行うときのキャッシュ識別名になる。指定しない場合はランダムな数列が割り当てられる。

* キャッシュサイズ上限 `Builder#maxCacheSize()`

    インデックスが保持できるキーの上限数を設定する。指定しない場合は Integer.MAX_VALUE か Long.MAX_VALUE。SIMPLE インデックス以外で有効。

* キャッシュ初期サイズ `Builder#initialCacheSize()`

    インデックスが初期状態で割り当てるテーブルのサイズを設定する。設定しない場合は 32。

* プロパティ

    初期化時に参照するプロパティを設定する。

* インデックス `Builder#indexType()`

    キャッシュのキーを管理する方法を決めるインデックスを指定する。いずれも列挙型 `Builder.IndexType` の値として定義されている。
    * SIMPLE

        サイズ制限を行わないインデックス。メモリが許す限りインデックスを利用できる。

    * LRU

        キャッシュサイズが `Builder.maxCacheSize()` で設定した値に達したとき、バリューをもっとも参照／更新していないキーが削除される。

    * FIFO

        キャッシュサイズが `Builder.maxCacheSize()` で設定した値に達したとき、バリューをもっとも更新していないキーが削除される。

    * FRAGILE_LRU

        同期を取らない LRU.

    * FRAGILE_FIFO

        同期を取らない FIFO.

    * LFU

        未実装。

    * AdaptiveReplacement

        未実装。

<a name="builder_spec_cacheaccessor"></a>

* キャッシュアクセッサ `Builder#cacheAccessorType()`

    キャッシュのバリューをストレージ（メモリ、ファイル）に格納する方式を指定する。いずれも列挙型 `Builder.CacheAccessorType` の値として定義されている。HEAP, BYTE\_BUFFER, MEMORY\_MAPPED\_FILE, FILE の4種類ある。
    * HEAP

        バリューの格納場所としてヒープを利用する。つまり、これを指定したときはヒープ外にデータを格納しない。

    * BYTE\_BUFFER

        バリューの格納場所として `java.nio.ByteBuffer` を利用する。使用するプロパティは以下のとおり。全体のサイズを指定する方法と、`ByteBuffer` を一つずつ指定する方法がある。まずは、全体サイズを指定する方法について説明する。
        * reservoir.ByteBufferCacheAccessor.direct

            ダイレクトバッファを利用する場合は true。デフォルトは false。つまり、trueを指定しないとヒープを利用したキャッシュとなる。
        * reservoir.ByteBufferCacheAccessor.blockSize

            最小の管理単位となるバイトブロックのサイズ。単位は byte。デフォルトは 512。

        * reservoir.ByteBufferCacheAccessor.partitions

            割り当てるバッファの個数。同一バッファへのアクセスは同期が取られるが、割り当てられたそれぞれのバッファは並列にアクセスされうる。
        * reservoir.ByteBufferCacheAccessor.size

            割り当てるバッファのサイズ。単位は byte。この値を blockSize で割った数、ただし、割り切れない場合は割った数 + 1 だけブロックが作成されるため、blockSizeで割り切れない値が指定されると、blockSize - (size % blockSize) だけ指定値よりも大きいバッファサイズを割り当てようとする。また、partitions で指定した値だけバッファの生成が行われ、その最小サイズは blockSize になるため、最小値は blockSize * partisions byte となる。デフォルトは 536870912 (512MB)。

        * reservoir.ByteBufferCacheAccessor.coder

            バリューをバッファへ格納するときに用いるシリアライザと、バリューをバッファから取得するときに用いるデシリアライザを規定する `Coder` のクラス名を指定する。詳細は、「[Coder 仕様](#coder_spec)」を参照。デフォルトは net.ihiroky.reservoir.coder.SerializableCoder。

        `ByteBuffer` を一つずつ指定する方法は以下のとおり。partition パラメータは 数字.direct, 数字.capacity というサブパラメータがついており、同じ数字がついている (direct, capacity) により一つの `ByteBuffer` に対する設定を記述する。
        * reservoir.ByteBufferCacheAccessor.blockSize

            最小の管理単位となるバイトブロックのサイズ。単位は byte。デフォルトは 512。

        * reservoir.ByteBufferCacheAccessor.partition.数字.direct

            数字で指定される `ByteBuffer` をダイレクトバッファとする場合は true。デフォルトは false。つまり、trueを指定しないとヒープを利用したキャッシュとなる。

        * reservoir.ByteBufferCacheAccessor.partition.数字.capacity

            数字で指定される `ByteBuffer` の容量を指定する。上限は 2^31 - 1。

    * MEMORY\_MAPPED\_FILE

        バリューの格納場所として `java.nio.channel.FileChannel` から生成される `java.nio.MappedByteBuffer` を利用する。使用するプロパティは以下のとおり。 file パラメータは 数字.path, 数字.size, 数字.mode というサブパラメータがついており、同じ数字がついている (path, size, mode) により一つのファイルに対する設定を記述する。
        * reservoir.MemoryMappedFileCacheAccessor.file.数字.path

            `MappedByteBuffer` の元となるファイルを示すパス。
        * reservoir.MemoryMappedFileCacheAccessor.file.数字.size

            path で示されたファイルのサイズ。単位は byte。ファイルサイズが size に満たない場合はsizeまで拡張され、ファイルサイズが size より大きい場合は、size まで縮小される。上限は 2^31 - 1。
        * reservoir.MemoryMappedFileCacheAccessor.file.数字.mode

            path で示されたファイルをオープンする時のモード。READ_WRITE, SYNC（ディスクとメモリを完全に同期）, DSYNC（コンテンツのみディスクとメモリを同期）のいずれか。READ_WRITE がデフォルト。

        * reservoir.MemoryMappedFileCacheAccessor.blockSize

            最小の管理単位となるバイトブロックのサイズ。単位は byte。デフォルトは 1024。
        * reservoir.MemoryMappedFileCacheAccessor.coder

            バリューをバッファへ格納するときに用いるシリアライザと、バリューをバッファから取得するときに用いるデシリアライザを規定する `Coder` のクラス名を指定する。詳細は、「[Coder 仕様](#coder_spec)」を参照。デフォルトは net.ihiroky.reservoir.coder.SerializableCoder。
    * FILE

        バリューの格納場所として、ファイルを利用する。使用するプロパティは以下のとおり。MEMORY_MAPPED_FILE と同様に、file パラメータは 数字.path, 数字.size, 数字.mode というサブパラメータがついており、同じ数字がついている (path, size, mode) により一つのファイルに対する設定を記述する。

        * reservoir.FileCacheAccessor.file.数字.path

            ファイルを示すパス。
        * reservoir.FileCacheAccessor.file.数字.size

            path で示されたファイルのサイズ。単位は byte。ファイルサイズが size に満たない場合はsizeまで拡張され、ファイルサイズが　size より大きい場合は、size まで縮小される。
        * reservoir.FileCacheAccessor.file.数字.mode

            path で示されたファイルをオープンする時のモード。READ_WRITE, SYNC（ディスクとメモリを完全に同期）, DSYNC（コンテンツのみディスクとメモリを同期）のいずれか。READ_WRITE がデフォルト。
        * reservoir.FileCacheAccessor.blockSize

            最小の管理単位となるバイトブロックのサイズ。単位は byte。デフォルトは 1024。
        * reservoir.FileCacheAccessor.coder

            バリューをバッファへ格納するときに用いるシリアライザと、バリューをバッファから取得するときに用いるデシリアライザを規定する `Coder` のクラス名を指定する。詳細は、「[Coder 仕様](#coder_spec)」を参照。デフォルトは net.ihiroky.reservoir.coder.SerializableCoder。

<a name="coder_spec"></a>

## Coder 仕様
キャッシュアクセッサのうちヒープにデータを載せるタイプ（`Builder#CacheAccessorType.HEAP`）以外は、ストレージにオブジェクト（データ）を格納する／ストレージからオブジェクト（データ）を取得するためにシリアライズ／デシリアライズを行う必要がある。これを取り扱うためのインターフェースが `net.ihiroky.reservoir.Coder` である。

### 既製 Coder
あらかじめ用意されている `Coder` 実装クラスは以下のとおり。また、それぞれに対し、`Builder#property()`により指定できるプロパティを示す。

* `net.ihiroky.reservoir.coder.ByteArrayCoder`

    バイト列をそのままストレージに格納／ストレージにから取得する。バイトの配列を扱うので、シリアライズ／デシリアライズ処理はない。

    * reservoir.ByteArrayCoder.compress.enabled

        圧縮の有効／無効設定。trueを指定すると `java.util.zip` による ZLIB 圧縮が有効になる。デフォルトは false。
    * reservoir.ByteArrayCoder.compress.level

        圧縮が有効な時、その圧縮レベルを指定する。1 〜 9 の値をとり、1は低圧縮率・高速で、昇順に高圧縮率・低速となる。デフォルトは 1。
    * reservoir.ByteArrayCoder.initByteSize

        圧縮を行うときに利用する一時バッファサイズ。指定値より大きいバッファが必要な時は自動的に拡張される。最小で16バイト、デフォルトは512バイト。
* `net.ihiroky.reservoir.coder.SerializableCoder`

    `java.io.Serializable` を実装したクラスのオブジェクトをストレージに格納／ストレージから取得する。Javaのシリアライズ／デシリアライズ機構を利用する。

    * reservoir.SerializableCoder.compress.enabled

        圧縮の有効／無効設定。trueを指定すると `java.util.zip` による ZLIB 圧縮が有効になる。デフォルトは false。
    * reservoir.SerializableCoder.compress.level

        圧縮が有効な時、その圧縮レベルを指定する。1 〜 9 の値をとり、1は低圧縮率・高速で、昇順に高圧縮率・低速となる。デフォルトは 1。
* `net.ihiroky.reservoir.coder.SimpleStringCoder`

    `String` をストレージに格納／ストレージから取得する。`char` を2バイトとみなして `String` を バイト列へ変換、またはその逆の変換を行うことでシリアライズ／デシリアライズを行う。

    * reservoir.SimpleStringCoder.compress.enabled

        圧縮の有効／無効設定。trueを指定すると `java.util.zip` による ZLIB 圧縮が有効になる。デフォルトは false。
    * reservoir.SimpleStringCoder.compress.level

        圧縮が有効な時、その圧縮レベルを指定する。1 〜 9 の値をとり、1は低圧縮率・高速で、昇順に高圧縮率・低速となる。デフォルトは 1。
* `net.ihiroky.reservoir.coder.StringCoder`

    `String` をストレージに格納／ストレージから取得する。UTF-8 または指定した文字コードに基づきバイト列への変換、またはその逆の変換を行うことでシリアライズ／デシリアライズを行う。

### カスタム Coder
`Coder` インターフェースがネストしているインターフェース `Encoder`, `Decoder` の実装を用意し、これを用いて `Coder` クラスを実装・利用することでシリアライズ／デシリアライズ処理を追加することができる。

### 圧縮サポート
`CompressionSupport` には圧縮・伸長ロジックを組み込む際に現れる汎用的な処理がまとめられている。
`CompressionSupport#loadProperties()` を `Coder#init()` から呼び出し状態（圧縮有効／無効、圧縮レベル設定）を初期化すると、`Coder#Encoder`, `Coder#Decoder` で以下のメソッドが使用できる。

* `CompressionSupport#createOutputStreamIfEnabled()`（圧縮有効時に圧縮ストリームを生成）
* `CompressionSupport#createInputStreamIfEnabled()`（圧縮有効時に伸長ストリームを生成）
* `CompressionSupport#createEncoderInfEnabled()`（圧縮有効時に圧縮エンコーダを生成）
* `CompressionSupport#createDecoderIfEnabled()`（圧縮有効時に伸長エンコーダを生成）

また、圧縮有効／無効、圧縮レベル設定をサポートするプロパティ（'指定プレフィクス.compress.enabled', '指定プレフィクス.compress.level'）が設定として利用できるようになる。例として、例えば既製 Coder の `ByteArrayCoder` がある。

## TODO
* LFU, Adaptive Replacement Key Priority インデックス実装  
* eviction (Time To Live, Time To Idle)  
* REST HTTPのエンジンに grizzly を使う
* REST pretty print
* REST で putIfAbsent, replace, remove  
* REST でキュー操作
