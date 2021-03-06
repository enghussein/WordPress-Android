package org.wordpress.android.viewmodel.pages

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import javax.inject.Inject
import org.wordpress.android.fluxc.action.PostAction
import org.wordpress.android.fluxc.action.PostAction.DELETE_POST
import org.wordpress.android.fluxc.action.PostAction.UPDATE_POST
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.util.coroutines.suspendCoroutineWithTimeout
import org.wordpress.android.viewmodel.pages.ActionPerformer.PageAction.EventType
import org.wordpress.android.viewmodel.pages.ActionPerformer.PageAction.EventType.UPLOAD
import kotlin.coroutines.experimental.Continuation

class ActionPerformer
@Inject constructor(private val dispatcher: Dispatcher) {
    private var continuation: Continuation<Boolean>? = null
    private lateinit var eventType: EventType

    companion object {
        private const val ACTION_TIMEOUT = 30L * 1000
    }

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun performAction(action: PageAction) {
        val success = suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) { uploadCont ->
            continuation = uploadCont
            eventType = action.event
            action.perform()
        }
        continuation = null

        if (success == true) {
            action.onSuccess()
        } else {
            action.onError()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostUploaded(event: OnPostUploaded) {
        if (continuation != null && eventType == UPLOAD) {
            continuation!!.resume(!event.isError)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostChange(event: OnPostChanged) {
        if (continuation != null) {
            val postAction = postCauseOfChangeToPostAction(event.causeOfChange)
            if (eventType.action == postAction) {
                continuation!!.resume(!event.isError)
            }
        }
    }

    private fun postCauseOfChangeToPostAction(postCauseOfChange: CauseOfOnPostChanged): PostAction =
            when (postCauseOfChange) {
                is CauseOfOnPostChanged.DeletePost -> PostAction.DELETE_POST
                CauseOfOnPostChanged.FetchPages -> PostAction.FETCH_PAGES
                CauseOfOnPostChanged.FetchPosts -> PostAction.FETCH_POST
                CauseOfOnPostChanged.RemoveAllPosts -> PostAction.REMOVE_ALL_POSTS
                is CauseOfOnPostChanged.RemovePost -> PostAction.REMOVE_POST
                is CauseOfOnPostChanged.UpdatePost -> PostAction.UPDATE_POST
            }

    data class PageAction(val event: EventType, val perform: () -> Unit) {
        var onSuccess: () -> Unit = { }
        var onError: () -> Unit = { }
        var undo: () -> Unit = { }

        enum class EventType(val action: PostAction?) {
            UPLOAD(null),
            UPDATE(UPDATE_POST),
            REMOVE(DELETE_POST)
        }
    }
}
