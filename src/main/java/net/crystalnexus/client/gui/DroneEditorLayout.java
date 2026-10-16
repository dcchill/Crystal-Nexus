package net.crystalnexus.client.gui;

public record DroneEditorLayout(int width,int height,int toolbarRows,int inspectorWidth,boolean overlayInspector) {
    public static DroneEditorLayout fit(int width,int height){
        int w=Math.max(180,Math.min(1200,width-16)),h=Math.max(180,Math.min(720,height-16));
        return new DroneEditorLayout(w,h,w<410?2:1,Math.min(224,Math.max(160,w/3)),w<600);
    }
    public int canvasTop(){return toolbarRows==1?80:104;}
    public static float clampZoom(float zoom){return Math.max(.25F,Math.min(3F,zoom));}
}
