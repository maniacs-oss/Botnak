package util.misc;

import gui.forms.GUIMain;
import util.Timer;
import util.Utils;
import util.settings.Settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Nick on 8/8/2014.
 */
public class Vote extends Thread {

    private int time, totalVotes;
    private Timer pollTime;
    private boolean isDone;
    private String channel;

    public boolean isDone() {
        return isDone;
    }

    public int getTotalVotes()
    {
        return totalVotes;
    }

    public List<Option> options;

    public Vote(String channel, int time, String... options) {
        this.channel = channel;
        isDone = false;
        this.time = time;
        this.totalVotes = 0;
        this.options = createOptions(options);
    }

    private List<Option> createOptions(String[] options)
    {
        List<Option> toReturn = new CopyOnWriteArrayList<>();
        for (int i = 0; i < options.length; i++) {
            Option o = new Option(options[i], i + 1, this);
            toReturn.add(o);
        }
        return toReturn;
    }

    public void addVote(String name, int option)
    {
        if (option > options.size() || option < 1) return;
        Option vote = getOption(name);
        if (vote != null) {//they already voted
            if (vote.compare != option) {//but now it's for a different
                vote.decrease(name);
                totalVotes--;
            } else {
                return; // They already voted for this!
            }
        }
        option--;
        Option newVote = options.get(option);
        newVote.increment(name);
        totalVotes++;

        // Update the GUI
        if (GUIMain.voteGUI != null && GUIMain.voteGUI.isVisible())
            GUIMain.voteGUI.updatePoll(this);
    }


    @Override
    public void start()
    {
        pollTime = new Timer(Utils.handleInt(time));
        printStart();
        super.start();
    }

    @Override
    public void run() {
        while (!GUIMain.shutDown && pollTime.isRunning()) {
            try {
                sleep(20);
            } catch (Exception ignored) {
            }
        }
        if (!isDone) {
            isDone = true;
            printResults();
        }
    }

    @Override
    public void interrupt() {
        isDone = true;
        super.interrupt();
    }

    /**
     * Gets an Option that the user is in, otherwise null.
     *
     * @param user The user to check.
     * @return The option they voted for, otherwise null.
     */
    private Option getOption(String user)
    {
        for (Option o : options) {
            if (o.count > 0) {
                for (String voter : o.voters) {
                    if (voter.equalsIgnoreCase(user)) {
                        return o;
                    }
                }
            }
        }
        return null;
    }

    public class Option implements Comparable<Option>
    {
        private String name;
        List<String> voters;
        int count = 0;
        int compare;
        private Vote parent;

        Option(String name, int compareIndex, Vote parent)
        {
            this.name = name;
            compare = compareIndex;
            voters = new ArrayList<>();
            this.parent = parent;
        }

        void increment(String name) {
            count++;
            voters.add(name);
        }

        void decrease(String name) {
            count--;
            voters.remove(name);
        }

        public int getCount()
        {
            return count;
        }

        public String getName()
        {
            return name;
        }

        public Vote getParent()
        {
            return parent;
        }

        @Override
        public int compareTo(Option o) {
            if (o.count > this.count) {
                return -1;
            } else if (o.count == this.count) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    public void printStart() {
        Settings.accountManager.getBot().sendMessage(channel, "Vote started! " +
                "Everybody has " + time + " seconds to vote! To vote, type \"!vote #\" where # represents your choice!");
        for (int i = 0; i < options.size(); i++) {
            Settings.accountManager.getBot().sendMessage(channel, (i + 1) + ": " + options.get(i).name);
        }
    }

    public void printResults() {
        String[] results = getResults();
        for (String s : results) {
            Settings.accountManager.getBot().sendMessage(channel, s);
        }

        if (GUIMain.voteGUI != null && GUIMain.voteGUI.isVisible())
            GUIMain.voteGUI.pollEnded(this);
    }

    public String[] getResults() {
        ArrayList<String> resultStrings = new ArrayList<>();
        resultStrings.add("The results to the poll are:");
        ArrayList<Option> results = getSortedOptions();
        int totalVotes = 0;
        for (Option o : results) {
            totalVotes += o.count;
        }
        if (totalVotes > 0) {
            for (Option o : results) {
                resultStrings.add(o.name + ": " + o.count + "/" + totalVotes + " (" + (int) (((double) o.count / (double) totalVotes) * 100) + "%)");
            }
        } else {
            resultStrings.add("Nobody voted for anything. BibleThump");
        }
        return resultStrings.toArray(new String[resultStrings.size()]);
    }

    private ArrayList<Option> getSortedOptions() {
        ArrayList<Option> results = new ArrayList<>();
        results.addAll(options);
        Collections.sort(results);//sort into ascending based on votes
        Collections.reverse(results);//make it descending
        return results;
    }
}