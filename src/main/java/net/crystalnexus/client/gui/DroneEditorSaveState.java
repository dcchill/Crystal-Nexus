package net.crystalnexus.client.gui;

/** Tracks edits made while a save acknowledgement is in flight. */
public final class DroneEditorSaveState {
    public boolean dirty,closing;
    public int pendingRevision=-1,request;
    private long edits,submittedEdits;

    public void changed(){edits++;dirty=true;}
    public void begin(int revision){pendingRevision=revision;submittedEdits=edits;request++;}
    public boolean acknowledge(int revision,int replyRequest,boolean accepted){
        if(pendingRevision<0||revision!=pendingRevision||replyRequest!=request)return false;
        pendingRevision=-1;
        if(accepted)dirty=edits!=submittedEdits;
        else closing=false;
        return true;
    }
    public void reset(){dirty=false;closing=false;pendingRevision=-1;}
    public boolean canClose(){return closing&&!dirty&&pendingRevision<0;}
}
