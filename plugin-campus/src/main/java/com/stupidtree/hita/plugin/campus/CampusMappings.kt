package com.stupidtree.hita.plugin.campus

import com.stupidtree.hita.api.CampusCardSpending
import com.stupidtree.hita.api.CampusCardSummary
import com.stupidtree.hita.api.CampusCardTransaction
import com.stupidtree.hita.api.CampusCardTransactions
import com.stupidtree.hita.api.CampusLabProgress
import com.stupidtree.hita.api.CampusLabSession
import com.stupidtree.hita.api.CampusLibrarySnapshot
import com.stupidtree.hita.api.CampusPodCategory
import com.stupidtree.hita.api.CampusPodContact
import com.stupidtree.hita.api.CampusPodContacts
import com.stupidtree.hita.api.CampusPodGroup
import com.stupidtree.hita.api.CampusPodReader
import com.stupidtree.hita.api.CampusPodReservation
import com.stupidtree.hita.api.CampusPodSpace
import com.stupidtree.hita.plugin.campus.model.CampusCardSpending as CardSpendingModel
import com.stupidtree.hita.plugin.campus.model.CampusCardSummary as CardSummaryModel
import com.stupidtree.hita.plugin.campus.model.CampusCardTransaction as CardTransactionModel
import com.stupidtree.hita.plugin.campus.model.CampusCardTransactions as CardTransactionsModel
import com.stupidtree.hita.plugin.campus.model.LabCourseProgress as LabProgressModel
import com.stupidtree.hita.plugin.campus.model.LabSessionItem as LabSessionModel
import com.stupidtree.hita.plugin.campus.model.LibrarySmartWallSnapshot as SmartWallModel
import com.stupidtree.hita.plugin.campus.model.SilentPodCategory as PodCategoryModel
import com.stupidtree.hita.plugin.campus.model.SilentPodContact as PodContactModel
import com.stupidtree.hita.plugin.campus.model.SilentPodContacts as PodContactsModel
import com.stupidtree.hita.plugin.campus.model.SilentPodGroup as PodGroupModel
import com.stupidtree.hita.plugin.campus.model.SilentPodReader as PodReaderModel
import com.stupidtree.hita.plugin.campus.model.SilentPodReservation as PodReservationModel
import com.stupidtree.hita.plugin.campus.model.SilentPodSpace as PodSpaceModel

/** 内部模型到公开契约的映射；只做字段搬移，不改变语义。 */
internal fun CardSummaryModel.toPublic(spending: CardSpendingModel?): CampusCardSummary =
    CampusCardSummary(
        balanceCents = balanceCents,
        statusText = statusText,
        processedAtText = processedAtText,
        spending = spending?.toPublic(),
    )

internal fun CardSpendingModel.toPublic(): CampusCardSpending =
    CampusCardSpending(
        yesterdayText = yesterdayText,
        weekText = weekText,
        monthText = monthText,
    )

internal fun CardTransactionsModel.toPublic(): CampusCardTransactions =
    CampusCardTransactions(items = items.map { it.toPublic() }, total = total)

internal fun CardTransactionModel.toPublic(): CampusCardTransaction =
    CampusCardTransaction(
        id = id,
        amountCents = amountCents,
        occurredAtText = occurredAtText,
        occurredAtMillis = occurredAtMillis,
        merchant = merchant,
        method = method,
    )

internal fun PodCategoryModel.toPublic(): CampusPodCategory = when (this) {
    PodCategoryModel.QUAD_LOCKED -> CampusPodCategory.QUAD_LOCKED
    PodCategoryModel.QUAD -> CampusPodCategory.QUAD
    PodCategoryModel.SINGLE -> CampusPodCategory.SINGLE
}

internal fun CampusPodCategory.toModel(): PodCategoryModel = when (this) {
    CampusPodCategory.QUAD_LOCKED -> PodCategoryModel.QUAD_LOCKED
    CampusPodCategory.QUAD -> PodCategoryModel.QUAD
    CampusPodCategory.SINGLE -> PodCategoryModel.SINGLE
}

internal fun PodSpaceModel.toPublic(): CampusPodSpace = CampusPodSpace(
    id = id,
    codeName = codeName,
    areaFullName = areaFullName,
    spaceTypeName = spaceTypeName,
    category = category.toPublic(),
    capacity = capacity,
    teamCapacity = teamCapacity,
    isTeam = isTeam,
    isAccessControl = isAccessControl,
    allowDurations = allowDurations,
    disabledDurations = disabledDurations,
    minDurationMinutes = minDurationMinutes,
    maxDurationMinutes = maxDurationMinutes,
)

internal fun CampusPodSpace.toModel(): PodSpaceModel = PodSpaceModel(
    id = id,
    codeName = codeName,
    areaFullName = areaFullName,
    spaceTypeName = spaceTypeName,
    category = category.toModel(),
    capacity = capacity,
    teamCapacity = teamCapacity,
    isTeam = isTeam,
    isAccessControl = isAccessControl,
    allowDurations = allowDurations,
    disabledDurations = disabledDurations,
    minDurationMinutes = minDurationMinutes,
    maxDurationMinutes = maxDurationMinutes,
)

internal fun PodGroupModel.toPublic(): CampusPodGroup =
    CampusPodGroup(category = category.toPublic(), spaces = spaces.map { it.toPublic() })

internal fun PodContactModel.toPublic(): CampusPodContact =
    CampusPodContact(id = id, name = name, cardNo = cardNo)

internal fun PodContactsModel.toPublic(): CampusPodContacts =
    CampusPodContacts(contacts = contacts.map { it.toPublic() }, defaultMobile = defaultMobile)

internal fun PodReaderModel.toPublic(): CampusPodReader =
    CampusPodReader(name = name, cardNo = cardNo)

internal fun PodReservationModel.toPublic(): CampusPodReservation = CampusPodReservation(
    id = id,
    spaceName = spaceName,
    areaName = areaName,
    spaceTypeName = spaceTypeName,
    date = date,
    startTime = startTime,
    endTime = endTime,
    typeText = typeText,
    status = status,
    statusText = statusText,
    cancelReason = cancelReason,
    isMain = isMain,
    isNeedSign = isNeedSign,
    isAllowCancel = isAllowCancel,
)

internal fun SmartWallModel.toPublic(): CampusLibrarySnapshot = CampusLibrarySnapshot(
    currentInLibrary = peoples.currentInLibrary,
    todayVisitors = peoples.todayVisitors,
    occupiedRooms = rooms.occupied,
    availableRooms = rooms.available,
    fetchedAtMillis = fetchedAtMillis,
)

internal fun LabProgressModel.toPublic(): CampusLabProgress = CampusLabProgress(
    termName = termName,
    currentWeek = currentWeek,
    sessions = sessions.map { it.toPublic() },
    fetchedAtMillis = fetchedAtMillis,
)

internal fun LabSessionModel.toPublic(): CampusLabSession = CampusLabSession(
    id = id,
    courseName = courseName,
    week = week,
    weekday = weekday,
    startSection = startSection,
    endSection = endSection,
    room = room,
)
