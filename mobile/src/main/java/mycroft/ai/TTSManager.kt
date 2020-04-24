/*
 *  Copyright (c) 2017. Mycroft AI, Inc.
 *
 *  This file is part of Mycroft-Android a client for Mycroft Core.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package mycroft.ai

import android.app.Activity
import android.app.Service
import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.EngineInfo
import android.util.Log
import java.lang.Float.parseFloat
import java.util.*
import kotlin.collections.HashMap


/**
 * TTSManager is a wrapper around the Android System's Text-To-Speech ('TTS')
 * API.
 *
 *
 * All constructors in this class require a context reference.
 * Make sure to clean up with [.shutDown] when the context's
 * [Activity.onDestroy] or [Service.onDestroy] method is called.
 *
 *
 * @see TextToSpeech
 *
 *
 * @author Paul Scott
 */

class TTSManager {

    /**
     * Backing TTS for this instance. Should not (ever) be null.
     */
    private lateinit var mTts: TextToSpeech
    private var testEngine: EngineInfo = EngineInfo()
    /**
     * Whether the TTS is available for use (i.e. loaded into memory)
     */
    private var isLoaded = false

    /**
     * External listener for error and success events. May be null.
     */
    private var mTTSListener: TTSListener? = null

    operator fun Bundle.set(key: String, value: String) = putString(key, value)

    //current API version is 19
    //var androidAPILevel = android.os.Build.VERSION.SDK_INT

    //paprams is used by the hashmap to set volume
    val params = HashMap<String, String>()





    var onInitListener: TextToSpeech.OnInitListener = TextToSpeech.OnInitListener { status ->
        if (status == TextToSpeech.SUCCESS) {
            val result = mTts.setLanguage(Locale.US)
            params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, "0.5") // change the 0.5 to any value from 0-1 (1 is default)

            isLoaded = true
            Log.i(TAG, "TTS initialized")

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                logError("This Language is not supported")
            }
        } else {
            logError("Initialization Failed!")
        }
    }

    /**
     * Create a new TTSManager attached to the given context.
     *
     * @param context any non-null context.
     */
    constructor(context: Context) {
        mTts = TextToSpeech(context, onInitListener)
    }

    /**
     * Special overload of constructor for testing purposes.
     *
     * @param textToSpeech the internal TTS this object will manage
     */
    constructor(textToSpeech: TextToSpeech) {
        mTts = textToSpeech
    }

    fun setTTSListener(mTTSListener: TTSListener) {
        this.mTTSListener = mTTSListener
    }

    /**
     * Wrapper for [TextToSpeech.shutdown]
     */
    fun shutDown() {
        mTts.shutdown()
    }

    fun addQueue(text: String, utteranceFrom: UtteranceFrom) {
        //if you say "soft" or "softer" it will recognize
        // change the speed of the audio output

        var finalText = text

        if (utteranceFrom == UtteranceFrom.USER) {
            //we are splitting the text input into an array
            val parts = finalText.split(" ").toMutableList()

            var speed = parts.indexOf("speed")

            if(speed != -1 && speed != parts.size-1){
                var speed1 = parts[speed+1].toFloatOrNull()
                if(speed1 != null){
                    mTts.setSpeechRate(speed1)
                    //parts.remove("speed")
                    //parts.remove(parts[speed])
                    //finalText = parts.joinToString(" ")
                }
            }

            var pitch = parts.indexOf("pitch")

            if(pitch != -1 && pitch != parts.size-1){
                var pitch1 = parts[pitch+1].toFloatOrNull()
                if(pitch1 != null){
                    mTts.setPitch(pitch1)
                    //parts.remove("pitch")
                    //parts.remove(parts[pitch])
                    //finalText = parts.joinToString(" ")
                }
            }

            var volume = parts.indexOf("toot")

            if(volume != -1 && volume != parts.size-1){
                if(parts[volume+1] != null){
                    params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, parts[volume+1]) // change the 0.5 to any value from 0-1 (1 is default)
                    //parts.remove("volume")
                    //parts.remove(parts[volume])
                    //finalText = parts.joinToString(" ")
                }
            }

            if (finalText.contains("normal")) {
                mTts.setSpeechRate(1.0F)
                mTts.setPitch(1.0F)
                params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, "0.5") // change the 0.5 to any value from 0-1 (1 is default)
            }

            if (finalText.contains("quickly")) {
                mTts.setSpeechRate(3.0F)
            }

            if (finalText.contains("slowly")) {
                mTts.setSpeechRate(0.5F)
            }

            // change the pitch of the audio output
            if (finalText.contains("low")) {
                mTts.setPitch(0.5F)
            }

            if (finalText.contains("high")) {
                mTts.setPitch(3F)
            }
            if (finalText.contains("soft")) {
                params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, "0.1") // change the 0.5 to any value from 0-1 (1 is default)
            }
            if (finalText.contains("loud")) {
                params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, "1.0") // change the 0.5 to any value from 0-1 (1 is default)
            }
        }

        if (isLoaded) {
            mTts.speak(finalText, TextToSpeech.QUEUE_ADD, params)
        }
        else {
            logError("TTS Not Initialized")
        }
    }

    fun initQueue(text: String) {
        if (isLoaded) {
            mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null)
        }
        else
            logError("TTS Not Initialized")
    }


    /**
     * Wrapper around [Log.e] that also notifies
     * the [.setTTSListener], if present.
     *
     * @param msg any non-null message
     */
    private fun logError(msg: String) {
        if (mTTSListener != null) {
            mTTSListener!!.onError(msg)
        }
        Log.e(TAG, msg)
    }

    interface TTSListener {
        fun onError(message: String)
    }

    companion object {

        private val TAG = "TTSManager"
    }
}
