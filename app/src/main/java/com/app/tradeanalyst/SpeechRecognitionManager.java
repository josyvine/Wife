package com.tradeanalyst.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import java.util.ArrayList;

public class SpeechRecognitionManager {

    public interface SpeechListener {
        void onResult(String text);
        void onStatusChanged(String status);
        void onError(String error);
    }

    private static final String TAG = "SpeechRecognition";
    private final Context mContext;
    private SpeechRecognizer mSpeechRecognizer;
    private final SpeechListener mListener;
    private boolean mIsListening = false;

    public SpeechRecognitionManager(Context context, SpeechListener listener) {
        mContext = context;
        mListener = listener;
        initRecognizer();
    }

    private void initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(mContext)) {
            mListener.onStatusChanged("System STT not supported dynamically. Fallback enabled.");
            return;
        }

        try {
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext);
            mSpeechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    mListener.onStatusChanged("Listening closely...");
                }

                @Override
                public void onBeginningOfSpeech() {
                    mListener.onStatusChanged("Processing audio...");
                }

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    mListener.onStatusChanged("Transcribing...");
                }

                @Override
                public void onError(int error) {
                    String message = getErrorMessage(error);
                    mIsListening = false;
                    mListener.onError(message);
                }

                @Override
                public void onResults(Bundle results) {
                    mIsListening = false;
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        mListener.onResult(matches.get(0));
                    } else {
                        mListener.onError("No speech recognized. Try again.");
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {}

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        } catch (Exception e) {
            Log.e(TAG, "SpeechRecognizer initialization failure", e);
            mListener.onStatusChanged("Voice engine fallback mode ready.");
        }
    }

    public void startListening() {
        if (mSpeechRecognizer == null) {
            mListener.onError("Speech Engine unavailable. Standard text command prompt is active!");
            return;
        }

        if (mIsListening) return;

        mIsListening = true;
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        try {
            mSpeechRecognizer.startListening(intent);
        } catch (Exception e) {
            mIsListening = false;
            mListener.onError("STT build start failed: " + e.getMessage());
        }
    }

    public void stopListening() {
        if (mSpeechRecognizer != null && mIsListening) {
            try {
                mSpeechRecognizer.stopListening();
            } catch (Exception ignored) {}
            mIsListening = false;
        }
    }

    public void destroy() {
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.destroy();
        }
    }

    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO: return "Audio recording error.";
            case SpeechRecognizer.ERROR_CLIENT: return "Client-side companion error.";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Record Audio permissions missing.";
            case SpeechRecognizer.ERROR_NETWORK: return "Internet connectivity error.";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Network request timeout.";
            case SpeechRecognizer.ERROR_NO_MATCH: return "No match recognized.";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Voice services busy.";
            case SpeechRecognizer.ERROR_SERVER: return "Google Voice servers unavailable.";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "Nothing heard.";
            default: return "STT Voice Engine code: " + errorCode;
        }
    }
}
