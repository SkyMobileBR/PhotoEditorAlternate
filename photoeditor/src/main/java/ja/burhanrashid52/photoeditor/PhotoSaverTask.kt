package ja.burhanrashid52.photoeditor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.View
import ja.burhanrashid52.photoeditor.BitmapUtil.removeTransparency
import ja.burhanrashid52.photoeditor.PhotoEditor.OnSaveListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Created by Burhanuddin Rashid on 18/05/21.
 *
 * @author <https:></https:>//github.com/burhanrashid52>
 */
internal class PhotoSaverTask(photoEditorView: PhotoEditorView, boxHelper: BoxHelper) {
    private var mSaveSettings: SaveSettings
    private var mOnSaveListener: OnSaveListener? = null
    private var mOnSaveBitmap: OnSaveBitmap? = null
    private val mPhotoEditorView: PhotoEditorView?
    private val mBoxHelper: BoxHelper
    private val mDrawingView: DrawingView
    fun setOnSaveListener(onSaveListener: OnSaveListener?) {
        mOnSaveListener = onSaveListener
    }

    fun setOnSaveBitmap(onSaveBitmap: OnSaveBitmap?) {
        mOnSaveBitmap = onSaveBitmap
    }

    fun setSaveSettings(saveSettings: SaveSettings) {
        mSaveSettings = saveSettings
    }

    private fun saveImageAsBitmap(): SaveResult {
        return if (mPhotoEditorView != null) {
            SaveResult(null, null, buildBitmap())
        } else {
            SaveResult(null, null, null)
        }
    }

    private fun saveImageInFile(mImagePath: String): SaveResult {
        val file = File(mImagePath)
        return try {
            val out = FileOutputStream(file, false)
            if (mPhotoEditorView != null) {
                val capturedBitmap = buildBitmap()
                capturedBitmap.compress(
                    mSaveSettings.compressFormat,
                    mSaveSettings.compressQuality,
                    out
                )
            }
            out.flush()
            out.close()
            Log.d(TAG, "Filed Saved Successfully")
            SaveResult(null, mImagePath, null)
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(TAG, "Failed to save File")
            SaveResult(e, mImagePath, null)
        }
    }

    private fun buildBitmap(): Bitmap {
        return if (mSaveSettings.isTransparencyEnabled) removeTransparency(
            captureView(
                mPhotoEditorView
            )
        ) else captureView(mPhotoEditorView)
    }

    private fun handleFileCallback(saveResult: SaveResult) {
        val exception = saveResult.mException
        val imagePath = saveResult.mImagePath
        if (exception == null) {
            //Clear all views if its enabled in save settings
            if (mSaveSettings.isClearViewsEnabled) {
                mBoxHelper.clearAllViews(mDrawingView)
            }
            if (mOnSaveListener != null) {
                assert(imagePath != null)
                mOnSaveListener!!.onSuccess(imagePath!!)
            }
        } else {
            if (mOnSaveListener != null) {
                mOnSaveListener!!.onFailure(exception)
            }
        }
    }

    private fun handleBitmapCallback(saveResult: SaveResult) {
        val bitmap = saveResult.mBitmap
        if (bitmap != null) {
            if (mSaveSettings.isClearViewsEnabled) {
                mBoxHelper.clearAllViews(mDrawingView)
            }
            if (mOnSaveBitmap != null) {
                mOnSaveBitmap!!.onBitmapReady(bitmap)
            }
        } else {
            if (mOnSaveBitmap != null) {
                mOnSaveBitmap!!.onFailure(Exception("Failed to load the bitmap"))
            }
        }
    }

    private fun captureView(view: View?): Bitmap {
        val bitmap = Bitmap.createBitmap(
            view!!.width,
            view.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        while (view.isDirty) {
            // Busy-wait (while View is Dirty and can't be drawn)
            Handler(Looper.getMainLooper()).postDelayed({}, 100)
            Log.d(TAG, "Generating final image...")
        }
        Log.d(TAG, "Final image about to be drawn in canvas...")
        view.draw(canvas)
        Log.d(TAG, "Final image drawn and ready for saving...")
        return bitmap
    }

    fun saveBitmap() {
        execute()
    }

    fun saveFile(imagePath: String?) {
        execute(imagePath)
    }

    fun execute(vararg inputs: String?) {
        // PRE-EXECUTE
        mBoxHelper.clearHelperBox()
/*        mDrawingView.destroyDrawingCache()*/

        // EXECUTE ON BACK
        // Create a media file name

        CoroutineScope(Dispatchers.IO).launch {
            val saveResult = if (inputs.isEmpty()) {
                saveImageAsBitmap()
            } else {
                saveImageInFile(inputs[0]!!)
            }

            // POST-EXECUTE
            if (TextUtils.isEmpty(saveResult.mImagePath)) {
                withContext(Dispatchers.Main) {
                    handleBitmapCallback(saveResult)
                }
            } else {
                withContext(Dispatchers.Main) {
                    handleFileCallback(saveResult)
                }
            }
        }

    }

    internal class SaveResult(
        val mException: Exception?,
        val mImagePath: String?,
        val mBitmap: Bitmap?
    )

    companion object {
        const val TAG = "PhotoSaverTask"
    }

    init {
        mPhotoEditorView = photoEditorView
        mDrawingView = photoEditorView.drawingView!!
        mBoxHelper = boxHelper
        mSaveSettings = SaveSettings.Builder().build()
    }
}