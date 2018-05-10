package com.example.codelab.sceneform

import android.net.Uri
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutCompat
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.LinearLayout
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable

import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import android.widget.Toast
import android.content.Intent
import android.support.v4.content.FileProvider
import android.view.PixelCopy
import android.os.HandlerThread
import android.graphics.Bitmap
import android.os.Environment
import android.os.Handler
import com.google.ar.sceneform.ArSceneView
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var fragment: ArFragment
    private val pointer = PointerDrawable()
    private var isTracking : Boolean = false
    private var isHitting : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fab = findViewById<View>(R.id.fab)
        fab.setOnClickListener({ takePhoto() })

        fragment = supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment
        fragment.arSceneView.scene.setOnUpdateListener({
            frameTime ->
            fragment.onUpdate(frameTime)
            onUpdate()
        })
        initializeGallery()
    }

    private fun takePhoto() {
        val filename = generateFilename()
        val view = fragment.arSceneView

        // Create a bitmap the size of the scene view.
        val bitmap = Bitmap.createBitmap(view.width, view.height,
                Bitmap.Config.ARGB_8888)

        // Create a handler thread to offload the processing of the image.
        val handlerThread = HandlerThread("PixelCopier")
        handlerThread.start()
        // Make the request to copy.
        PixelCopy.request(view, bitmap, { copyResult ->
            if (copyResult === PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename)
                } catch (e: IOException) {
                    val toast = Toast.makeText(this@MainActivity, e.toString(),
                            Toast.LENGTH_LONG)
                    toast.show()
                    return@request
                }

                val snackbar = Snackbar.make(findViewById(android.R.id.content),
                        "Photo saved", Snackbar.LENGTH_LONG)
                snackbar.setAction("Open in Photos") { v ->
                    val photoFile = File(filename)

                    val photoURI = FileProvider.getUriForFile(this@MainActivity,
                            this@MainActivity.packageName + ".ar.codelab.name.provider",
                            photoFile)
                    val intent = Intent(Intent.ACTION_VIEW, photoURI)
                    intent.setDataAndType(photoURI, "image/*")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)

                }
                snackbar.show()
            } else {
                val toast = Toast.makeText(this@MainActivity,
                        "Failed to copyPixels: $copyResult", Toast.LENGTH_LONG)
                toast.show()
            }
            handlerThread.quitSafely()
        }, Handler(handlerThread.looper))
    }

    private fun generateFilename(): String {
        val date = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES).absolutePath + File.separator + "Sceneform/" + date + "_screenshot.jpg"
    }

    @Throws(IOException::class)
    private fun saveBitmapToDisk(bitmap: Bitmap, filename: String) {

        val out = File(filename)
        if (!out.parentFile.exists()) {
            out.parentFile.mkdirs()
        }
        try {
            FileOutputStream(filename).use({ outputStream ->
                ByteArrayOutputStream().use({ outputData ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData)
                    outputData.writeTo(outputStream)
                    outputStream.flush()
                    outputStream.close()
                })
            })
        } catch (ex: IOException) {
            throw IOException("Failed to save bitmap to disk", ex)
        }

    }

    fun initializeGallery() {
        val gallery = findViewById<LinearLayout>(R.id.gallery_layout)

        val andy = ImageView(this)
        andy.setImageResource(R.drawable.droid_thumb)
        andy.contentDescription = "andy"
        andy.setOnClickListener({view ->
            addObject(Uri.parse("andy.sfb"))
        })
        gallery.addView(andy)

        val cabin = ImageView(this)
        cabin.setImageResource(R.drawable.cabin_thumb)
        cabin.contentDescription = "cabin"
        cabin.setOnClickListener({view ->
            addObject(Uri.parse("Cabin.sfb"))
        })
        gallery.addView(cabin)

        val house = ImageView(this)
        house.setImageResource(R.drawable.house_thumb)
        house.contentDescription = "house"
        house.setOnClickListener({view ->
            addObject(Uri.parse("House.sfb"))
        })
        gallery.addView(house)


        val igloo = ImageView(this)
        igloo.setImageResource(R.drawable.igloo_thumb)
        igloo.contentDescription = "igloo"
        igloo.setOnClickListener({view ->
            addObject(Uri.parse("igloo.sfb"))
        })
        gallery.addView(igloo)

    }





    private fun addObject(model: Uri) {

        val frame = fragment.arSceneView.arFrame
        val pt = getScreenCenter()
        var hits : List<HitResult>? = null
        if (frame != null){
            hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat())
            for (hit in hits){
                val trackable = hit.trackable
                if (trackable is Plane && (trackable as Plane).isPoseInPolygon(hit.hitPose)){
                    placeObject(fragment, hit.createAnchor(), model)
                    break
                }
            }
        }

    }

    private fun placeObject(fragment: ArFragment, anchor: Anchor, model: Uri) {
        val renderableFuture = ModelRenderable.builder()
                .setSource(fragment.context, model)
                .build()
                .thenAccept({renderable: ModelRenderable? ->
                    addNodetoScene(fragment, anchor, renderable)}
                ).exceptionally { throwable: Throwable ->
                    val builder = AlertDialog.Builder(this)
                    builder.setMessage(throwable.message)
                            .setTitle("Codelab error")
                            val dialog = builder.create()
                    dialog.show()
                    null
                }
    }

    private fun addNodetoScene(fragment: ArFragment, anchor: Anchor, renderable: ModelRenderable?) {
        val anchorNode = AnchorNode(anchor)
        val node = TransformableNode(fragment.transformationSystem)
        node.renderable = renderable
        node.setParent(anchorNode)
        fragment.arSceneView.scene.addChild(anchorNode)
        node.select()
    }


    fun onUpdate() {
        val trackingChanged = updateTracking()
        val contentView : View = findViewById(android.R.id.content)
        if (trackingChanged) {
            if (isTracking) {
                contentView.overlay.add(pointer)
            } else {
                contentView.overlay.remove(pointer)
            }
            contentView.invalidate()
        }

        if (isTracking) {
            val hitTestChanged = updateHitTest()
            if (hitTestChanged) {
                pointer.enabled = isHitting
                contentView.invalidate()
            }
        }
    }

    private fun updateHitTest(): Boolean {
        val frame = fragment.arSceneView.arFrame
        val pt = getScreenCenter()
        var hits : List<HitResult>? = null
        val wasHitting = isHitting
        if (frame != null){
            hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat())
            for (hit in hits){
                val trackable = hit.trackable
                if (trackable is Plane
                        && (trackable as Plane).isPoseInPolygon(hit.hitPose)){
                    isHitting = true
                    break
                }
            }
        }
        return wasHitting != isHitting
    }

    private fun getScreenCenter() : android.graphics.Point {
        val vw : View = findViewById(android.R.id.content);
        return android.graphics.Point(vw.width / 2, vw.height / 2)

    }

    fun updateTracking() : Boolean {
        val frame = fragment.arSceneView.arFrame
        val wasTracking = isTracking
        isTracking = frame.camera.trackingState == TrackingState.TRACKING
        return isTracking != wasTracking
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)

    }
}
