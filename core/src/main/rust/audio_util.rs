use std::f64::consts::PI;
use songbird::driver::opus::{Channels, SampleRate};
use tracing::{trace, warn};

/// 1 second of RawAudio
pub const MAX_AUDIO_BUFFER: usize = 50;
pub const RAW_AUDIO_SIZE: usize = 960;
/// 20 ms of 16-bit PCM
pub type RawAudio = [i16; RAW_AUDIO_SIZE];

pub const OPUS_SAMPLE_RATE: SampleRate = SampleRate::Hz48000;
pub const OPUS_CHANNELS: Channels = Channels::Stereo;

pub const SAMPLE_RATE: u32 = OPUS_SAMPLE_RATE as i32 as u32;
pub const CHANNELS: u32 = OPUS_CHANNELS as i32 as u32;
const MAX_BEHIND_MUFFLING_PERCENT: f64 = 0.25; // 25% maximum muffling

pub fn combine_audio_parts(parts: Vec<RawAudio>) -> RawAudio {
    // Based on https://github.com/DV8FromTheWorld/JDA/blob/11c5bf02a1f4df3372ab68e0ccb4a94d0db368df/src/main/java/net/dv8tion/jda/internal/audio/AudioConnection.java#L529
    let Some(max_length) = parts.iter().map(|p| p.len()).max() else {
        // .max() gives None on empty
        return [0; RAW_AUDIO_SIZE];
    };
    trace!(max_length);
    let mut mixed = [0; RAW_AUDIO_SIZE];
    let mut sample: i32;
    for i in 0..max_length {
        if i >= mixed.len() {
            warn!(len = mixed.len(), "Audio parts are bigger than 20ms! Some audio may be lost. Please report to GitHub Issues!");
            break;
        }
        sample = 0;
        for part in &parts {
            // We don't need to check part.len() against i, because,
            // unlike Java, we have a guarantee part is of length
            // RAW_AUDIO_SIZE and we already checked that i isn't above that 😎
            sample += part[i] as i32;
        }
        if sample > i16::MAX as i32 {
            mixed[i] = i16::MAX;
        } else if sample < i16::MIN as i32 {
            mixed[i] = i16::MIN;
        } else {
            mixed[i] = sample as i16
        }
    }
    mixed
}

/// volume should be between 0 and 1
pub fn adjust_volume(audio: &mut RawAudio, volume: f64) {
    for sample in audio {
        let res = (*sample as f64 * volume).round();
        if res > i16::MAX as f64 {
            *sample = i16::MAX;
        } else if res < i16::MIN as f64 {
            *sample = i16::MIN;
        } else {
            *sample = res as i16;
        }
    }
}

/// Adjusts the panning to produce stereo audio
/// Slightly muffles audio that is behind the user to give a better circular audio experience
pub fn adjust_panning(audio: &mut RawAudio, panning: f64) {
    let normalized_panning = panning.sin();
    let behind_muffle: f64 = if panning.abs() > PI / 2.0 {
        let percent_behind = (panning.abs() - PI / 2.0) / (PI / 2.0);
        MAX_BEHIND_MUFFLING_PERCENT + (1.0 - MAX_BEHIND_MUFFLING_PERCENT) * (1.0 - percent_behind)
    } else {
        1.0 // No muffle
    };

    let left_multiplier = 1.0 - normalized_panning;
    let right_multiplier = 1.0 + normalized_panning;

    // Iterate to adjust with interleaving format
    for i in (0..audio.len()).step_by(2) {
        // Adjust the left channel
        let left_sample = audio[i] as f64 * left_multiplier * behind_muffle;
        audio[i] = left_sample.clamp(i16::MIN as f64, i16::MAX as f64) as i16;

        // Adjust the right channel
        let right_sample = audio[i + 1] as f64 * right_multiplier * behind_muffle;
        audio[i + 1] = right_sample.clamp(i16::MIN as f64, i16::MAX as f64) as i16;
    }
}