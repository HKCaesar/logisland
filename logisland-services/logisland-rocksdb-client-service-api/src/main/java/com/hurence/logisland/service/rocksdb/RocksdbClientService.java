package com.hurence.logisland.service.rocksdb;


import com.hurence.logisland.annotation.documentation.CapabilityDescription;
import com.hurence.logisland.annotation.documentation.Tags;
import com.hurence.logisland.component.AllowableValue;
import com.hurence.logisland.component.PropertyDescriptor;
import com.hurence.logisland.controller.ControllerService;
import com.hurence.logisland.service.rocksdb.delete.DeleteRangeRequest;
import com.hurence.logisland.service.rocksdb.delete.DeleteRangeResponse;
import com.hurence.logisland.service.rocksdb.delete.DeleteRequest;
import com.hurence.logisland.service.rocksdb.delete.DeleteResponse;
import com.hurence.logisland.service.rocksdb.put.ValuePutRequest;
import com.hurence.logisland.service.rocksdb.get.GetRequest;
import com.hurence.logisland.service.rocksdb.get.GetResponse;
import com.hurence.logisland.service.rocksdb.scan.RocksIteratorHandler;
import com.hurence.logisland.service.rocksdb.scan.RocksIteratorRequest;
import com.hurence.logisland.validator.StandardValidators;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteOptions;

import java.util.Collection;
import java.util.List;

/**
 * **********WORK IN PROGRESS************
 * This is the interface for the rocksDb service.
 * Here some thinking I got
 *
 *
 * RocksDb a besoin d'un path pour écrire ses fichiers... Du coup j'ai plusieurs interrogations.
 _Ce path devrait être générer automatiquement par le service, l'utilisateur donne juste un nom pour la db ?
 _Ou bien l'utilisateur donne directe le path (qui serait du coup en mode sandbox)
 Dans tous les cas y'a le problème ou plusieurs utilisateurs d'un même cluster choisissent le même path sans le vouloir... Dans ce cas il faudrait rajouter un mot de passe (qu'il faudrait donc gérer nous même...) ? Ou quelque chose pour pouvoir enlever cette ambiguité ?
 On est d'accord que le service rocksDb finalement serait une sorte de zookeeper pour nos processeurs qui auraient besoin de persistence ? J'avoue que c'est assez compliqué donc des choses m'échappent probablement
 Sinon pour utiliser rocksDb uniquement in-memory je ne crois pas que ce soit possible à part en utilisant tmpfs/ramfs.
 Donc plus je me renseigne sur la techno plus je me dis que ce n'est pas du tout fait pour faire du pure caching.
 *
 */

@Tags({"elasticsearch", "client"})
@CapabilityDescription("A controller service for accessing an elasticsearch client.")
public interface RocksdbClientService extends ControllerService {

    //TODO should the path be sandboxed somewhere specific ?
    PropertyDescriptor ROCKSDB_PATH = new PropertyDescriptor.Builder()
            .name("rocksdb.path")
            .description("strategy for compaction")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    PropertyDescriptor ROCKSDB_READONLY = new PropertyDescriptor.Builder()
            .name("rocksdb.readonly")
            .description("Should the database be opened in readOnly mode ? You can use only one instance in read and write mode")//TODO look in documentation i dont remember well
            .required(false)
            .defaultValue("false")
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    ///////////////////////////////////////
    // Properties of the column families //
    ///////////////////////////////////////
    /*
    You must specify all family currrently present in the database if you want to use the database in read and write mode.
    */

    PropertyDescriptor FAMILY_NAMES = new PropertyDescriptor.Builder()
            .name("rocksdb.family.name")
            .description("Comma-separated list of family names in rocksdb. You must specify all family currrently present in the database if you want to use the database in read and write mode (default).")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    /////////////////////////////////////////
    // Properties of the compaction policy //
    /////////////////////////////////////////

    AllowableValue UNIVERSAL_COMPACTION_POLICY = new AllowableValue("kCompactionStyleUniversal", "Universal compaction policy",
            "TODO");//TODO

    AllowableValue LEVEL_COMPACTION_POLICY = new AllowableValue("kCompactionStyleLevel", "Level compaction policy",
            "TODO.");//TODO

    PropertyDescriptor COMPACTION_POLICY = new PropertyDescriptor.Builder()
            .name("compaction.policy")
            .description("strategy for compaction   ")
            .required(false)
            .allowableValues(UNIVERSAL_COMPACTION_POLICY, LEVEL_COMPACTION_POLICY)
            .defaultValue(LEVEL_COMPACTION_POLICY.getValue())//current rocksDb default (5.4.0)
            .build();

    PropertyDescriptor AUTOMATIC_COMPACTION = new PropertyDescriptor.Builder()
            .name("compaction.automatic")
            .description("Disable automatic compactions. Manual compactions can still be issued on this database.")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .defaultValue("true")//current rocksDb default (5.4.0)
            .build();
    //TODO
    PropertyDescriptor COMPACTION_FILTER = new PropertyDescriptor.Builder()
            .name("compaction.filter")
            .description("Allows an application to modify/delete a key-value during background compaction. The client must provide compaction_filter_factory if it requires a new compaction filter to be used for different compaction processes. Client should specify only one of filter or factory. ")
            .required(false)
            //.addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            //.defaultValue("true")//current rocksDb default (5.4.0)
            .build();
    //TODO
    PropertyDescriptor COMPACTION_FILTER_FACTORY = new PropertyDescriptor.Builder()
            .name("compaction.filter.factory")
            .description("a factory that provides compaction filter objects which allow an application to modify/delete a key-value during background compaction.")
            .required(false)
            //.addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            //.defaultValue("true")//current rocksDb default (5.4.0)
            .build();

    //TODO
//    Options::access_hint_on_compaction_start - Specify the file access pattern once a compaction is started. It will be applied to all input files of a compaction. Default: NORMAL
//    Options::level0_file_num_compaction_trigger - Number of files to trigger level-0 compaction. A negative value means that level-0 compaction will not be triggered by number of files at all.
//            Options::target_file_size_base and Options::target_file_size_multiplier - Target file size for compaction. target_file_size_base is per-file size for level-1. Target file size for level L can be calculated by target_file_size_base * (target_file_size_multiplier ^ (L-1)) For example, if target_file_size_base is 2MB and target_file_size_multiplier is 10, then each file on level-1 will be 2MB, and each file on level 2 will be 20MB, and each file on level-3 will be 200MB. Default target_file_size_base is 2MB and default target_file_size_multiplier is 1.
//    Options::max_compaction_bytes - Maximum number of bytes in all compacted files. We avoid expanding the lower level file set of a compaction if it would make the total compaction cover more than this amount.
//            Options::max_background_compactions - Maximum number of concurrent background jobs, submitted to the default LOW priority thread pool
//    Options::compaction_readahead_size - If non-zero, we perform bigger reads when doing compaction. If you're running RocksDB on spinning disks, you should set this to at least 2MB. We enforce it to be 2MB if you don't set it with direct I/O.

    /////////////////////////////////////////////
    // Properties of Leveled policy compaction //
    /////////////////////////////////////////////

    //TODO

    ///////////////////////////////////////////////
    // Properties of Universal policy compaction //
    ///////////////////////////////////////////////

    //TODO

    //////////////////////////////////////////
    // Properties of Fifo policy compaction //
    //////////////////////////////////////////

    //TODO

    //////////////////////////////////////////
    // Properties of BlockBasedTable Format //
    //////////////////////////////////////////

    //TODO

    //////////////////////////////////////////
    // Properties of Table options //
    //////////////////////////////////////////

    PropertyDescriptor CACHE_ENABLED = new PropertyDescriptor.Builder()
            .name("rocksdb.cache.enabled")
            .description("Should rocksDb uses cache ?")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .defaultValue("true")//current rocksDb default (5.4.0)
            .build();
    //TODO
    PropertyDescriptor UNCOMPRESSED_CACHE = new PropertyDescriptor.Builder()
            .name("rocksdb.cache.uncompressed")
            .description("Cache that will be used for rocksDb database to save uncompressed data in memory")
            .required(false)
            //.addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            //.defaultValue("true")//current rocksDb default (5.4.0)
            .build();
    //TODO
    PropertyDescriptor COMPRESSED_CACHE = new PropertyDescriptor.Builder()
            .name("rocksdb.cache.compressed")
            .description("Cache that will be used for rocksDb database to save compressed data in memory")
            .required(false)
            //.addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            //.defaultValue("true")//current rocksDb default (5.4.0)
            .build();

    //////////////////////////////////////
    // Properties of the backoff policy //
    //////////////////////////////////////

    AllowableValue NO_BACKOFF_POLICY = new AllowableValue("noBackoff", "No retry policy",
            "when a request fail there won't be any retry.");

    AllowableValue CONSTANT_BACKOFF_POLICY = new AllowableValue("constantBackoff", "wait a fixed amount of time between retries",
            "wait a fixed amount of time between retries, using user put retry number and throttling delay");

    AllowableValue EXPONENTIAL_BACKOFF_POLICY = new AllowableValue("exponentialBackoff", "custom exponential policy",
            "time waited between retries grow exponentially, using user put retry number and throttling delay");

    AllowableValue DEFAULT_EXPONENTIAL_BACKOFF_POLICY = new AllowableValue("defaultExponentialBackoff", "es default exponential policy",
            "time waited between retries grow exponentially, using es default parameters");

    PropertyDescriptor BULK_BACK_OFF_POLICY = new PropertyDescriptor.Builder()
            .name("backoff.policy")
            .description("strategy for retrying to execute requests in bulkRequest")
            .required(true)
            .allowableValues(NO_BACKOFF_POLICY, CONSTANT_BACKOFF_POLICY, EXPONENTIAL_BACKOFF_POLICY, DEFAULT_EXPONENTIAL_BACKOFF_POLICY)
            .defaultValue(DEFAULT_EXPONENTIAL_BACKOFF_POLICY.getValue())
            .build();

    PropertyDescriptor BULK_RETRY_NUMBER = new PropertyDescriptor.Builder()
            .name("num.retry")
            .description("number of time we should try to inject a bulk into es")
            .required(true)
            .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR)
            .defaultValue("3")
            .build();

    PropertyDescriptor BULK_THROTTLING_DELAY = new PropertyDescriptor.Builder()
            .name("throttling.delay")
            .description("number of time we should wait between each retry (in milliseconds)")
            .required(true)
            .addValidator(StandardValidators.POSITIVE_LONG_VALIDATOR)
            .defaultValue("500")
            .build();

    ////////////////////////////////////////////////
    // Properties of elasticsearch bulk processor //
    ////////////////////////////////////////////////


    //////////////////////
    // Other properties //
    //////////////////////










    ///////////////
    // DBOptions //
    ///////////////

    PropertyDescriptor OPTIMIZE_FOR_SMALL_DB = new PropertyDescriptor.Builder()
            .name("native.rocksdb.optimize_for_small_db")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor INCREASE_PARALLELISM = new PropertyDescriptor.Builder()
            .name("native.rocksdb.increase_parallelism")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.INTEGER_VALIDATOR)//TODO check all int types
            .build();
    PropertyDescriptor CREATE_IF_MISSING = new PropertyDescriptor.Builder()
            .name("native.rocksdb.create_if_missing")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor CREATE_MISSING_COLUMN_FAMILIES = new PropertyDescriptor.Builder()
            .name("native.rocksdb.create_missing_column_families")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor ERROR_IF_EXISTS = new PropertyDescriptor.Builder()
            .name("native.rocksdb.error_if_exists")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor PARANOID_CHECKS = new PropertyDescriptor.Builder()
            .name("native.rocksdb.paranoid_checks")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor MAX_OPEN_FILES = new PropertyDescriptor.Builder()
            .name("native.rocksdb.max_open_files")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .build();
    PropertyDescriptor MAX_FILE_OPENING_THREADS = new PropertyDescriptor.Builder()
            .name("native.rocksdb.max_file_opening_threads")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .build();
    PropertyDescriptor MAX_TOTAL_WAL_SIZE = new PropertyDescriptor.Builder()
            .name("native.rocksdb.max_total_wal_size")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .build();
    PropertyDescriptor USE_FSYNC = new PropertyDescriptor.Builder()
            .name("native.rocksdb.use_fsync")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor DB_LOG_DIR = new PropertyDescriptor.Builder()
            .name("native.rocksdb.db_log_dir")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    PropertyDescriptor WAL_DIR = new PropertyDescriptor.Builder()
            .name("native.rocksdb.wal_dir")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    PropertyDescriptor DELETE_OBSOLETE_FILES_PERIOD_MICROS = new PropertyDescriptor.Builder()
            .name("native.rocksdb.delete_obsolete_files_period_micros")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.LONG_VALIDATOR)
            .build();
    PropertyDescriptor BASE_BACKGROUND_COMPACTIONS = new PropertyDescriptor.Builder()
            .name("native.rocksdb.base_background_compactions")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .build();
    PropertyDescriptor MAX_BACKGROUND_COMPACTIONS = new PropertyDescriptor.Builder()
            .name("native.rocksdb.max_background_compactions")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .build();
    PropertyDescriptor MAX_SUBCOMPACTIONS = new PropertyDescriptor.Builder()
            .name("native.rocksdb.max_subcompactions")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .build();
    PropertyDescriptor MAX_BACKGROUND_FLUSHES = new PropertyDescriptor.Builder()
            .name("native.rocksdb.max_background_flushes")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .build();
    PropertyDescriptor MAX_LOG_FILE_SIZE = new PropertyDescriptor.Builder()
            .name("native.rocksdb.max_log_file_size")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .build();
    PropertyDescriptor LOG_FILE_TIME_TO_ROLL = new PropertyDescriptor.Builder()
            .name("native.rocksdb.log_file_time_to_roll")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.LONG_VALIDATOR)
            .build();
    PropertyDescriptor KEEP_LOG_FILE_NUM = new PropertyDescriptor.Builder()
            .name("native.rocksdb.keep_log_file_num")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.LONG_VALIDATOR)
            .build();
    PropertyDescriptor RECYCLE_LOG_FILE_NUM = new PropertyDescriptor.Builder()
            .name("native.rocksdb.recycle_log_file_num")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.LONG_VALIDATOR)
            .build();
    PropertyDescriptor MAX_MANIFEST_FILE_SIZE = new PropertyDescriptor.Builder()
            .name("native.rocksdb.max_manifest_file_size")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.LONG_VALIDATOR)
            .build();
    PropertyDescriptor TABLE_CACHE_NUMSHARDBITS = new PropertyDescriptor.Builder()
            .name("native.rocksdb.table_cache_numshardbits")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .build();
    PropertyDescriptor WAL_TTL_SECONDS = new PropertyDescriptor.Builder()
            .name("native.rocksdb.wal_ttl_seconds")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.LONG_VALIDATOR)
            .build();
    PropertyDescriptor WAL_SIZE_LIMIT_MB = new PropertyDescriptor.Builder()
            .name("native.rocksdb.wal_size_limit_mb")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.LONG_VALIDATOR)
            .build();
    PropertyDescriptor MANIFEST_PREALLOCATION_SIZE = new PropertyDescriptor.Builder()
            .name("native.rocksdb.manifest_reallocation_size")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.LONG_VALIDATOR)
            .build();
    PropertyDescriptor USE_DIRECT_READS = new PropertyDescriptor.Builder()
            .name("native.rocksdb.use_direct_reads")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor USE_DIRECT_IO_FOR_FLUSH_AND_COMPACTION = new PropertyDescriptor.Builder()
            .name("native.rocksdb.use_direct_io_for_flush_and_compaction")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor ALLOW_F_ALLOCATE = new PropertyDescriptor.Builder()
            .name("native.rocksdb.allow_f_allocate")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor ALLOW_MMAP_READS = new PropertyDescriptor.Builder()
            .name("native.rocksdb.allow_mmap_reads")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor ALLOW_MMAP_WRITES = new PropertyDescriptor.Builder()
            .name("native.rocksdb.allow_mmap_writes")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor IS_FD_CLOSE_ON_EXEC = new PropertyDescriptor.Builder()
            .name("native.rocksdb.is_fd_close_on_exec")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor STATS_DUMP_PERIOD_SEC = new PropertyDescriptor.Builder()
            .name("native.rocksdb.stats_dump_period_sec")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .build();
    PropertyDescriptor ADVISE_RANDOM_ON_OPEN = new PropertyDescriptor.Builder()
            .name("native.rocksdb.advise_random_on_open")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor DB_WRITE_BUFFER_SIZE = new PropertyDescriptor.Builder()
            .name("native.rocksdb.db_write_buffer_size")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.LONG_VALIDATOR)
            .build();
    PropertyDescriptor NEW_TABLE_READER_FOR_COMPACTION_INPUTS = new PropertyDescriptor.Builder()
            .name("native.rocksdb.new_table_reader_for_compaction_inputs")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor COMPACTION_READAHEAD_SIZE = new PropertyDescriptor.Builder()
            .name("native.rocksdb.compaction_readahead_size")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.LONG_VALIDATOR)
            .build();
    PropertyDescriptor RANDOM_ACCESS_MAX_BUFFER_SIZE = new PropertyDescriptor.Builder()
            .name("native.rocksdb.random_access_max_buffer_size")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.LONG_VALIDATOR)
            .build();
    PropertyDescriptor WRITABLE_FILE_MAX_BUFFER_SIZE = new PropertyDescriptor.Builder()
            .name("native.rocksdb.writable_file_max_buffer_size")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.LONG_VALIDATOR)
            .build();
    PropertyDescriptor USE_ADAPTIVE_MUTEX = new PropertyDescriptor.Builder()
            .name("native.rocksdb.use_adaptive_mutex")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor BYTES_PER_SYNC = new PropertyDescriptor.Builder()
            .name("native.rocksdb.bytes_per_sync")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.LONG_VALIDATOR)
            .build();
    PropertyDescriptor WAL_BYTES_PER_SYNC = new PropertyDescriptor.Builder()
            .name("native.rocksdb.wal_bytes_per_sync")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.LONG_VALIDATOR)
            .build();
    PropertyDescriptor ENABLE_THREAD_TRACKING = new PropertyDescriptor.Builder()
            .name("native.rocksdb.enable_thread_tracking")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor DELAYED_WRITE_RATE = new PropertyDescriptor.Builder()
            .name("native.rocksdb.delayed_write_rate")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.LONG_VALIDATOR)
            .build();
    PropertyDescriptor ALLOW_CONCURRENT_MEMTABLE_WRITE = new PropertyDescriptor.Builder()
            .name("native.rocksdb.allow_concurrent_memtable_write")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor ENABLE_WRITE_THREAD_ADAPTIVE_YIELD = new PropertyDescriptor.Builder()
            .name("native.rocksdb.allow_write_thread_adaptive_yield")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor WRITE_THREAD_MAX_YIELD_USEC = new PropertyDescriptor.Builder()
            .name("native.rocksdb.write_thread_max_yield_usec")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.LONG_VALIDATOR)
            .build();
    PropertyDescriptor WRITE_THREAD_SLOW_YIELD_USEC = new PropertyDescriptor.Builder()
            .name("native.rocksdb.write_thread_slow_yield_usec")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.LONG_VALIDATOR)
            .build();
    PropertyDescriptor SKIP_STATS_UPDATE_ON_DB_OPEN = new PropertyDescriptor.Builder()
            .name("native.rocksdb.skip_stats_update_on_db_open")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor ALLOW_2_PC = new PropertyDescriptor.Builder()
            .name("native.rocksdb.allow_2_pc")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor FAIL_IF_OPTIONS_FILE_ERROR = new PropertyDescriptor.Builder()
            .name("native.rocksdb.fail_if_options_file_error")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor DUMP_MALLOC_STATS = new PropertyDescriptor.Builder()
            .name("native.rocksdb.dump_malloc_stats")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor AVOID_FLUSH_DURING_RECOVERY = new PropertyDescriptor.Builder()
            .name("native.rocksdb.avoid_flush_during_recovery")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    PropertyDescriptor AVOID_FLUSH_DURING_SHUTDOWN = new PropertyDescriptor.Builder()
            .name("native.rocksdb.avoid_flush_during_shutdown")
            .description("TODO")
            .required(false)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();


    ///////////////////////
    // Family properties //
    ///////////////////////


    ////////////////////
    // Put operations //
    ////////////////////

    /**
     * Puts a batch of key value pairs in their column family using specific write option
     *
     * @param puts a list of put mutations
     * @throws RocksDBException thrown when there are communication errors with RocksDb
     */
    void put(Collection<ValuePutRequest> puts) throws RocksDBException;

    /**
     * Puts a key value pairs in their column family
     *
     * @param family family to put data in
     * @param key the key of the value to store
     * @param value the value to store in the specified family
     * @throws RocksDBException thrown when there are communication errors with RocksDb
     */
    void put(String familyName, byte[] key, byte[] value) throws RocksDBException;

    /**
     * Puts a batch of key value pairs in 'default' column family
     *
     * @param key the key of the value to store
     * @param value the value to store in the specified family
     * @throws RocksDBException thrown when there are communication errors with RocksDb
     */
    void put(byte[] key, byte[] value) throws RocksDBException;

    /**
     * Puts a key value pairs in their column family using specific option
     *
     * @param family family to put data in
     * @param key the key of the value to store
     * @param value the value to store in the specified family
     * @param writeOptions
     * @throws RocksDBException thrown when there are communication errors with RocksDb
     */
    void put(String familyName, byte[] key, byte[] value, WriteOptions writeOptions) throws RocksDBException;

    /**
     * Puts a key value pairs in 'default' column family using specific option
     *
     * @param key the key of the value to store
     * @param value the value to store in the specified family
     * @param writeOptions
     * @throws RocksDBException thrown when there are communication errors with RocksDb
     */
    void put(byte[] key, byte[] value, WriteOptions writeOptions) throws RocksDBException;

    ////////////////////
    // get operations //
    ////////////////////

    /**
     *
     * @param getRequests
     * @return
     * @throws RocksDBException thrown when there are communication errors with RocksDb
     */
    Collection<GetResponse> multiGet(Collection<GetRequest> getRequests) throws RocksDBException;

    /**
     *
     * @param key
     * @return
     * @throws RocksDBException thrown when there are communication errors with RocksDb
     */
    byte[] get(byte[] key) throws RocksDBException;

    /**
     *
     * @param key
     * @param rOption
     * @return
     * @throws RocksDBException thrown when there are communication errors with RocksDb
     */
    byte[] get(byte[] key, ReadOptions rOption) throws RocksDBException;

    /**
     *
     * @param family
     * @param key
     * @return
     * @throws RocksDBException thrown when there are communication errors with RocksDb
     */
    byte[] get(String familyName, byte[] key) throws RocksDBException;

    /**
     *
     * @param family
     * @param key
     * @param rOption
     * @return
     * @throws RocksDBException thrown when there are communication errors with RocksDb
     */
    byte[] get(String familyName, byte[] key, ReadOptions rOption) throws RocksDBException;

    ///////////////////////
    // remove operations //
    ///////////////////////

    Collection<DeleteResponse> multiDelete(Collection<DeleteRequest> deleteRequests) throws RocksDBException;
    void delete(byte[] key) throws RocksDBException;
    void delete(byte[] key, WriteOptions wOption) throws RocksDBException;
    void delete(String familyName, byte[] key) throws RocksDBException;
    void delete(String familyName, byte[] key, WriteOptions wOption) throws RocksDBException;

    /////////////////////////////
    // singleDelete operations //
    /////////////////////////////

    //TODO but it seems really very very specific and has many constraint
    //https://github.com/facebook/rocksdb/wiki/Single-Delete

    ///////////////////////////////
    // delete range operations ////
    ///////////////////////////////

    Collection<DeleteRangeResponse> multiDeleteRange(Collection<DeleteRangeRequest> deleteRangeRequests) throws RocksDBException;
    void deleteRange(byte[] keyStart, byte[] keyEnd) throws RocksDBException;
    void deleteRange(byte[] keyStart, byte[] keyEnd, WriteOptions wOption) throws RocksDBException;
    void deleteRange(String familyName, byte[] keyStart, byte[] keyEnd) throws RocksDBException;
    void deleteRange(String familyName, byte[] keyStart, byte[] keyEnd, WriteOptions wOption) throws RocksDBException;

    ///////////////////////////////////////
    // scan operations (with iterator) ////
    ///////////////////////////////////////
    /**
     * Scans the given table using the optional filter criteria and passing each result to the provided handler.
     *
     * @param handler  a handler to process iterators from rocksdb
     * @throws RocksDBException thrown when there are communication errors with RocksDb
     */
    void scan(RocksIteratorHandler handler) throws RocksDBException;
    /**
     * Scans the given table using the optional filter criteria and passing each result to the provided handler.
     *
     * @param familyName the column family to scan over
     * @param handler  a handler to process iterators from rocksdb
     * @throws RocksDBException thrown when there are communication errors with RocksDb
     */
    void scan(String familyName, RocksIteratorHandler handler) throws RocksDBException;

    /**Properties
     * Scans the given table for the given rowId and passes the result to the handler.
     *
     * @param familyName the column family to scan over
     * @param rOptions readOptions
     * @param handler a handler to process iterators from rocksdb
     * @throws RocksDBException thrown when there are communication errors with RocksDb
     */
    void scan(String familyName, ReadOptions rOptions, RocksIteratorHandler handler) throws RocksDBException;


}