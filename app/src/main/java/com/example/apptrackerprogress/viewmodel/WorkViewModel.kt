package com.example.apptrackerprogress.viewmodel

import android.content.Context
import android.net.Uri
import com.example.apptrackerprogress.data.WorkRepository
import com.example.apptrackerprogress.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class WorkViewModel : BaseViewModel() {
    val works: StateFlow<List<WorkItem>> = WorkRepository.works.asState(emptyList())

    fun detail(workId: String): Flow<WorkDetail?> = WorkRepository.detail(workId)

    fun addWork(form: WorkForm, onSuccess: () -> Unit, onError: (String) -> Unit) =
        action(onError, onSuccess) { WorkRepository.addWork(it, form) }

    fun deleteWork(work: WorkItem, returnStock: Boolean, onSuccess: () -> Unit, onError: (String) -> Unit) =
        action(onError, onSuccess) { WorkRepository.deleteWork(it, work, returnStock) }

    fun updateStatus(work: WorkItem, newStatus: String, onError: (String) -> Unit) =
        action(onError) { WorkRepository.updateStatus(it, work, newStatus) }

    fun updatePayment(workId: String, paid: Boolean, method: String, onError: (String) -> Unit) =
        action(onError) { WorkRepository.updatePayment(workId, paid, method) }

    fun addLineItem(workId: String, description: String, type: String, quantity: Int, unitPrice: Double, onSuccess: () -> Unit, onError: (String) -> Unit) =
        action(onError, onSuccess) { WorkRepository.addLineItem(workId, description, type, quantity, unitPrice) }

    fun deleteLineItem(workId: String, item: WorkLineItem, onError: (String) -> Unit) =
        action(onError) { WorkRepository.deleteLineItem(workId, item) }

    fun addProgressUpdate(
        context: Context, workId: String, workLabel: String, mediaUri: Uri?, comment: String,
        usedItems: List<UsedItem>, publicVisible: Boolean, onSuccess: () -> Unit, onError: (String) -> Unit
    ) = action(onError, onSuccess) {
        WorkRepository.addProgressUpdate(context.applicationContext, it, workId, workLabel, mediaUri, comment, usedItems, publicVisible)
    }

    fun setUpdateVisibility(workId: String, updateId: String, publicVisible: Boolean, onError: (String) -> Unit) =
        action(onError) { WorkRepository.setUpdateVisibility(workId, updateId, publicVisible) }

    fun deleteProgressUpdate(workId: String, workLabel: String, update: ProgressUpdate, onSuccess: () -> Unit, onError: (String) -> Unit) =
        action(onError, onSuccess) { WorkRepository.deleteProgressUpdate(it, workId, workLabel, update) }

    fun generateClientLink(workId: String): String = WorkRepository.clientLink(workId)
}
