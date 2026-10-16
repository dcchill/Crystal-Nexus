package net.crystalnexus.world.inventory;

import net.crystalnexus.entity.LUNADroneEntity;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.crystalnexus.network.DroneState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.UUID;

public final class DroneMenu extends AbstractContainerMenu {
    public final UUID droneId;
    public final LUNADroneEntity drone;
    public final ItemStackHandler cards;
    public CompoundTag state;
    public boolean cargoTab;
    public int submittedRevision=-1,editRequest;
    public boolean editAccepted;
    public String editError="";
    private final Player viewer;
    private int ticks;
    public DroneMenu(int id,Inventory inv,FriendlyByteBuf data){this(id,inv,data.readUUID(),data.readNbt());}
    private DroneMenu(int id,Inventory inv,UUID uuid,CompoundTag initial){
        super(CrystalnexusModMenus.DRONE.get(),id);droneId=uuid;drone=null;state=initial==null?new CompoundTag():initial;viewer=inv.player;
        cards=new ItemStackHandler(9);if(state.contains("cards"))cards.deserializeNBT(inv.player.registryAccess(),state.getCompound("cards"));slots(inv,new ItemStackHandler(9));
    }
    public DroneMenu(int id,Inventory inv,LUNADroneEntity drone){
        super(CrystalnexusModMenus.DRONE.get(),id);this.drone=drone;droneId=drone.getUUID();state=drone.view();viewer=inv.player;cards=drone.destinationCards;slots(inv,drone.inventory);
    }
    private void slots(Inventory inv,ItemStackHandler storage){
        for(int i=0;i<9;i++)addSlot(new SlotItemHandler(storage,i,18+i*18,59){
            @Override public boolean isActive(){return cargoTab;}
            @Override public boolean mayPlace(ItemStack stack){return cargoTab&&super.mayPlace(stack);}
            @Override public boolean mayPickup(Player p){return cargoTab&&stillValid(p);}
        });
        for(int i=0;i<9;i++)addSlot(new SlotItemHandler(cards,i,18+i*18,92){
            @Override public boolean isActive(){return cargoTab;}
            @Override public boolean mayPlace(ItemStack stack){return cargoTab&&stack.is(net.crystalnexus.init.CrystalnexusModItems.LINK_CARD.get());}
            @Override public boolean mayPickup(Player p){return cargoTab&&stillValid(p);}
            @Override public int getMaxStackSize(){return 1;}
            @Override public int getMaxStackSize(ItemStack stack){return 1;}
        });
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)playerSlot(inv,x+y*9+9,18+x*18,136+y*18);
        for(int x=0;x<9;x++)playerSlot(inv,x,18+x*18,194);
    }
    private void playerSlot(Inventory inv,int slot,int x,int y){addSlot(new Slot(inv,slot,x,y){
        @Override public boolean isActive(){return cargoTab;}
        @Override public boolean mayPlace(ItemStack stack){return cargoTab;}
        @Override public boolean mayPickup(Player p){return cargoTab&&stillValid(p);}
    });}
    public CompoundTag snapshot(){
        CompoundTag p=drone.view();if(submittedRevision>=0){p.putInt("submittedRevision",submittedRevision);p.putInt("editRequest",editRequest);p.putBoolean("editAccepted",editAccepted);p.putString("editError",editError);}return p;
    }
    @Override public boolean stillValid(Player player){return drone==null?player.level().isClientSide:drone.isAlive()&&drone.ownedBy(player)&&drone.level()==player.level();}
    @Override public void clicked(int slot,int button,ClickType type,Player player){if(cargoTab&&stillValid(player))super.clicked(slot,button,type,player);}
    @Override public void broadcastChanges(){super.broadcastChanges();if(++ticks%10==0&&viewer instanceof ServerPlayer player&&stillValid(player))PacketDistributor.sendToPlayer(player,new DroneState(containerId,snapshot()));}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!cargoTab||!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var original=slot.getItem();var copy=original.copy();boolean moved;
        if(index<18)moved=moveItemStackTo(original,18,slots.size(),true);
        else if(original.is(net.crystalnexus.init.CrystalnexusModItems.LINK_CARD.get()))moved=moveItemStackTo(original,9,18,false);
        else moved=moveItemStackTo(original,0,9,false);
        if(!moved)return ItemStack.EMPTY;if(original.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,original);return copy;
    }
}
