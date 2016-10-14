package local.is161505.uebung3;

/**
 * Created by n17405180 on 14.10.16.
 */

public class AudioPlayerPosition {

    private int position;

    private int duration;

    public AudioPlayerPosition(int position, int duration) {
        this.position = position;
        this.duration = duration;
    }


    public int getDuration() {
        return duration;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return "AudioPlayerPosition: " + (position/1000) + "s /" + (duration/1000) + "s";
    }
}
