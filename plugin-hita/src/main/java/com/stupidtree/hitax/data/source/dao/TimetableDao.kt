package com.stupidtree.hitax.data.source.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.stupidtree.hitax.data.model.timetable.Timetable

@Dao
interface TimetableDao {


    /**
     * 根据教务代码查找课表。
     *
     * 旧接口忽略来源：同一 code 有多个来源时优先返回迁移前的旧行（providerId 为 NULL），
     * 否则返回最近开始时间的行。新代码需要精确来源时用 [getTimetableByEasCodeAndProviderSync]。
     */
    @Query(
        "SELECT * FROM timetable WHERE code is :easCode " +
            "ORDER BY (providerId IS NULL) DESC, -startTime LIMIT 1"
    )
    fun getTimetableByEASCodeSync(easCode: String): Timetable?

    @Query(
        "SELECT * FROM timetable WHERE code is :easCode " +
            "ORDER BY (providerId IS NULL) DESC, -startTime LIMIT 1"
    )
    fun getTimetableByEASCode(easCode: String): LiveData<Timetable?>

    /** 按 (教务代码, 来源) 精确查找课表。 */
    @Query("SELECT * FROM timetable WHERE code is :easCode AND providerId is :providerId LIMIT 1")
    fun getTimetableByEasCodeAndProviderSync(easCode: String, providerId: String): Timetable?

    /** 查找同教务代码下尚未标注来源的旧行。 */
    @Query("SELECT * FROM timetable WHERE code is :easCode AND providerId is null ORDER BY -startTime LIMIT 1")
    fun getLegacyTimetableByEasCodeSync(easCode: String): Timetable?

    @Query("SELECT * FROM timetable order by -startTime")
    fun getTimetables(): LiveData<List<Timetable>>

    @Query("SELECT * FROM timetable order by -startTime")
    fun getTimetablesSync(): List<Timetable>

    @Query("SELECT * FROM timetable WHERE id is :id")
    fun getTimetableById(id: String): LiveData<Timetable>

    @Query("SELECT * FROM timetable WHERE id is :id")
    fun getTimetableByIdSync(id: String): Timetable?

    /**
     *  获取所有使用默认名字的课表名字
     */
    @Query("SELECT name from timetable where name like :defaultName")
    fun getTimetableNamesWithDefaultSync(defaultName: String): List<String>

    /**
     * 获得离某时间戳最近的时间表
     */
    @Query("SELECT * from timetable where (:ts-startTime in (select min(:ts-startTime) from timetable where :ts>startTime)) limit 1")
    fun getTimetableClosestToTimestamp(ts: Long): LiveData<Timetable?>

    @Query("SELECT * from timetable where (:ts-startTime in (select min(:ts-startTime) from timetable where :ts>startTime)) limit 1")
    fun getTimetableClosestToTimestampSync(ts: Long): Timetable?


    @Query("select count(*) from timetable")
    fun geeTimetableCount(): LiveData<Int>

    @Query("select count(*) from timetable")
    fun getTimetableCountSync(): Int

    @Query("select DISTINCT place from events where place like :string ")
    fun searchLocation(string: String): LiveData<List<String>>

    @Query("select DISTINCT place from events where place like :string ")
    fun searchLocationSync(string: String): List<String>


    /**
     * 保存课表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveTimetableSync(data: Timetable)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveTimetablesSync(data: List<Timetable>)

    @Delete
    fun deleteTimetablesSync(timetables: List<Timetable>)

    @Query("delete from timetable where id in (:ids)")
    fun deleteTimetablesInIdsSync(ids: List<String>)


    @Query("SELECT * FROM timetable WHERE id in (:ids)")
    fun getTimetablesInIdsSync(ids: List<String>): List<Timetable>



    @Query("delete from timetable")
    fun clear()
}