package net.crystalnexus.world.inventory;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.crystalnexus.util.DepotNetwork;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DepotMenu extends RecipeBookMenu<CraftingInput, CraftingRecipe> {
    public static final int PAGE_SIZE = 54;
    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 140;

    private final SimpleContainer depotView = new SimpleContainer(PAGE_SIZE);
    private final ResourceLocation[] depotItemIds = new ResourceLocation[PAGE_SIZE];
    private final TransientCraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();
    private final Player player;
    private final boolean craftingUnlocked;
    private final int depotStart;
    private final int playerStart;
    private String search = "";
    private int page;
    private boolean placingRecipe;

    public DepotMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, extraData.readBlockPos(), extraData.readBoolean(), extraData.readBoolean());
    }

    public DepotMenu(int id, Inventory inv) {
        this(id, inv, false, false);
    }

    public DepotMenu(int id, Inventory inv, boolean uplink, boolean craftingUnlocked) {
        this(id, inv, null, uplink, craftingUnlocked);
    }

    private DepotMenu(int id, Inventory inv, Object ignoredPos, boolean uplink, boolean craftingUnlocked) {
        super(CrystalnexusModMenus.DEPOT.get(), id);
        this.player = inv.player;
        this.craftingUnlocked = uplink && craftingUnlocked;

        if (this.craftingUnlocked) {
            addSlot(new ResultSlot(inv.player, craftSlots, resultSlots, 0, 290, 69) {
                @Override
                public void onTake(Player craftingPlayer, ItemStack stack) {
                    List<ItemStack> template = craftSlots.getItems().stream()
                            .map(item -> item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(1)).toList();
                    super.onTake(craftingPlayer, stack);
                    if (craftingPlayer instanceof ServerPlayer serverPlayer) refillCraftingGrid(serverPlayer, template);
                }
            });
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    addSlot(new Slot(craftSlots, col + row * 3, 200 + col * 18, 51 + row * 18));
                }
            }
        }
        depotStart = slots.size();

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9;
                addSlot(new Slot(depotView, index, 8 + col * 18, 18 + row * 18));
            }
        }
        playerStart = slots.size();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9 + 9;
                addSlot(new Slot(inv, index, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, PLAYER_INV_X + col * 18, PLAYER_INV_Y + 58));
        }
    }

    public boolean hasCraftingUpgrade() {
        return craftingUnlocked;
    }

    public boolean isDepotSlot(Slot slot) {
        return slot.container == depotView;
    }

    private boolean isDepotSlotId(int slotId) {
        return slotId >= depotStart && slotId < depotStart + PAGE_SIZE;
    }

    public void setDepotPage(String search, int page, List<DepotSavedData.Entry> entries) {
        this.search = search == null ? "" : search;
        this.page = Math.max(0, page);
        depotView.clearContent();
        Arrays.fill(depotItemIds, null);

        for (int i = 0; i < Math.min(entries.size(), PAGE_SIZE); i++) {
            DepotSavedData.Entry entry = entries.get(i);
            Item item = BuiltInRegistries.ITEM.get(entry.itemId());
            if (item == null || item == Items.AIR) continue;
            depotItemIds[i] = entry.itemId();
            depotView.setItem(i, new ItemStack(item, (int) Math.min(entry.count(), item.getDefaultInstance().getMaxStackSize())));
        }
        broadcastChanges();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < depotStart && !craftingAvailable(player)) return;
        if (isDepotSlotId(slotId)) {
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            if (!DepotSavedData.hasPoweredController(serverPlayer)) {
                serverPlayer.closeContainer();
                return;
            }

            DepotSavedData depot = DepotSavedData.get(serverPlayer);
            int depotIndex = slotId - depotStart;
            if (clickType == ClickType.QUICK_MOVE) {
                withdrawStack(serverPlayer, depot, depotItemIds[depotIndex]);
            } else if (clickType == ClickType.PICKUP && (button == 0 || button == 1)) {
                pickupOrDeposit(depot, depotItemIds[depotIndex], button);
            } else if (clickType == ClickType.PICKUP_ALL) {
                fillCarriedStack(depot, depotItemIds[depotIndex]);
            }
            refreshDepot(depot);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private boolean craftingAvailable(Player player) {
        if (!craftingUnlocked) return false;
        if (!(player instanceof ServerPlayer serverPlayer)) return true;
        if (DepotSavedData.hasPoweredController(serverPlayer) && DepotNetwork.hasCraftingUpgrade(serverPlayer)) return true;
        serverPlayer.closeContainer();
        return false;
    }

    private void pickupOrDeposit(DepotSavedData depot, ResourceLocation itemId, int button) {
        ItemStack carried = getCarried();
        if (!carried.isEmpty()) {
            ResourceLocation carriedId = BuiltInRegistries.ITEM.getKey(carried.getItem());
            int amount = button == 1 ? 1 : carried.getCount();
            carried.shrink((int) depot.deposit(carriedId, amount));
            setCarried(carried);
            return;
        }

        if (itemId == null) return;
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == Items.AIR) return;
        int available = (int) Math.min(depot.getCount(itemId), item.getDefaultInstance().getMaxStackSize());
        int amount = button == 1 ? (available + 1) / 2 : available;
        long removed = depot.remove(itemId, amount);
        if (removed > 0) setCarried(new ItemStack(item, (int) removed));
    }

    private void fillCarriedStack(DepotSavedData depot, ResourceLocation itemId) {
        ItemStack carried = getCarried();
        if (carried.isEmpty() || itemId == null || !itemId.equals(BuiltInRegistries.ITEM.getKey(carried.getItem()))) return;
        int amount = Math.min(carried.getMaxStackSize() - carried.getCount(), (int) Math.min(Integer.MAX_VALUE, depot.getCount(itemId)));
        carried.grow((int) depot.remove(itemId, amount));
        setCarried(carried);
    }

    private static ItemStack withdrawStack(ServerPlayer player, DepotSavedData depot, ResourceLocation itemId) {
        if (itemId == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;

        int amount = (int) Math.min(depot.getCount(itemId), item.getDefaultInstance().getMaxStackSize());
        if (amount <= 0) return ItemStack.EMPTY;
        depot.remove(itemId, amount);

        ItemStack stack = new ItemStack(item, amount);
        ItemStack original = stack.copy();
        player.getInventory().add(stack);
        if (!stack.isEmpty()) depot.add(itemId, stack.getCount());
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return true;
        return DepotSavedData.hasPoweredController(serverPlayer)
                && (!craftingUnlocked || DepotNetwork.hasCraftingUpgrade(serverPlayer));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!(player instanceof ServerPlayer serverPlayer) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        if (!DepotSavedData.hasPoweredController(serverPlayer)) {
            serverPlayer.closeContainer();
            return ItemStack.EMPTY;
        }
        DepotSavedData depot = DepotSavedData.get(serverPlayer);

        if (craftingUnlocked && index == 0) {
            Slot result = slots.get(index);
            if (!result.hasItem()) return ItemStack.EMPTY;
            ItemStack stack = result.getItem();
            ItemStack original = stack.copy();
            if (!moveItemStackTo(stack, playerStart, slots.size(), true)) return ItemStack.EMPTY;
            result.onQuickCraft(stack, original);
            if (stack.isEmpty()) result.setByPlayer(ItemStack.EMPTY);
            else result.setChanged();
            result.onTake(player, stack);
            if (!stack.isEmpty()) player.drop(stack, false);
            return original;
        }
        if (craftingUnlocked && index < depotStart) {
            Slot slot = slots.get(index);
            if (!slot.hasItem()) return ItemStack.EMPTY;
            ItemStack original = slot.getItem().copy();
            returnToDepot(serverPlayer, depot, slot.getItem());
            if (slot.getItem().isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            refreshDepot(depot);
            return original;
        }
        if (isDepotSlotId(index)) {
            ItemStack moved = withdrawStack(serverPlayer, depot, depotItemIds[index - depotStart]);
            refreshDepot(depot);
            return moved;
        }

        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (depot.tryDepositAll(stack) <= 0) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        refreshDepot(depot);
        return original;
    }

    @Override
    public void slotsChanged(Container container) {
        if (!placingRecipe && craftingUnlocked && !player.level().isClientSide) updateCraftingResult(null);
    }

    private void updateCraftingResult(RecipeHolder<CraftingRecipe> hint) {
        ServerPlayer serverPlayer = (ServerPlayer) player;
        CraftingInput input = craftSlots.asCraftInput();
        Optional<RecipeHolder<CraftingRecipe>> match = player.level().getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, player.level(), hint);
        ItemStack result = ItemStack.EMPTY;
        if (match.isPresent() && resultSlots.setRecipeUsed(player.level(), serverPlayer, match.get())) {
            ItemStack assembled = match.get().value().assemble(input, player.level().registryAccess());
            if (assembled.isItemEnabled(player.level().enabledFeatures())) result = assembled;
        }
        resultSlots.setItem(0, result);
        setRemoteSlot(0, result);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, result));
    }

    @Override
    public void handlePlacement(boolean maxTransfer, RecipeHolder<?> holder, ServerPlayer serverPlayer) {
        if (!craftingAvailable(serverPlayer) || !(holder.value() instanceof CraftingRecipe recipe)) return;
        DepotSavedData depot = DepotSavedData.get(serverPlayer);
        returnCraftingGrid(serverPlayer, depot);

        StackedContents contents = new StackedContents();
        depot.fillStackedContents(contents);
        int amount = maxTransfer ? contents.getBiggestCraftableStack(holder, new IntArrayList()) : 1;
        if (amount <= 0) {
            refreshDepot(depot);
            return;
        }

        IntArrayList choices = new IntArrayList();
        if (!contents.canCraft(recipe, choices, amount)) {
            refreshDepot(depot);
            return;
        }
        for (int choice : choices) {
            ItemStack stack = StackedContents.fromStackingIndex(choice);
            if (!stack.isEmpty()) amount = Math.min(amount, stack.getMaxStackSize());
        }
        choices.clear();
        if (amount <= 0 || !contents.canCraft(recipe, choices, amount)) {
            refreshDepot(depot);
            return;
        }

        Map<ResourceLocation, Integer> required = new HashMap<>();
        for (int choice : choices) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(StackedContents.fromStackingIndex(choice).getItem());
            required.merge(itemId, amount, Integer::sum);
        }
        if (required.entrySet().stream().anyMatch(entry -> depot.getCount(entry.getKey()) < entry.getValue())) {
            refreshDepot(depot);
            return;
        }
        required.forEach(depot::remove);

        placingRecipe = true;
        try {
            placeRecipe(recipe, choices, amount);
        } catch (RuntimeException exception) {
            clearCraftingContent();
            required.forEach(depot::add);
            throw exception;
        } finally {
            placingRecipe = false;
        }
        updateCraftingResult((RecipeHolder<CraftingRecipe>) holder);
        refreshDepot(depot);
    }

    private void placeRecipe(CraftingRecipe recipe, IntArrayList choices, int amount) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        int choiceIndex = 0;
        for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
            if (ingredients.get(ingredientIndex).isEmpty()) continue;
            int target = recipe instanceof ShapedRecipe
                    ? ingredientIndex % width + ingredientIndex / width * 3
                    : choiceIndex;
            ItemStack stack = StackedContents.fromStackingIndex(choices.getInt(choiceIndex++)).copyWithCount(amount);
            craftSlots.setItem(target, stack);
        }
    }

    private void returnCraftingGrid(ServerPlayer serverPlayer, DepotSavedData depot) {
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack stack = craftSlots.removeItemNoUpdate(i);
            returnToDepot(serverPlayer, depot, stack);
        }
        resultSlots.clearContent();
    }

    private void refillCraftingGrid(ServerPlayer player, List<ItemStack> template) {
        DepotSavedData depot = DepotSavedData.get(player);
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack ingredient = template.get(i);
            if (!craftSlots.getItem(i).isEmpty() || ingredient.isEmpty()) continue;
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(ingredient.getItem());
            if (depot.remove(itemId, 1) > 0) craftSlots.setItem(i, ingredient.copy());
        }
    }

    private static void returnToDepot(ServerPlayer player, DepotSavedData depot, ItemStack stack) {
        depot.tryDepositAll(stack);
        if (!stack.isEmpty() && !player.getInventory().add(stack)) player.drop(stack, false);
    }

    private void refreshDepot(DepotSavedData depot) {
        setDepotPage(search, page, depot.page(search, page, PAGE_SIZE));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (craftingUnlocked && player instanceof ServerPlayer serverPlayer) {
            DepotSavedData depot = DepotSavedData.get(serverPlayer);
            returnCraftingGrid(serverPlayer, depot);
        }
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedContents contents) {
        craftSlots.fillStackedContents(contents);
        if (player instanceof ServerPlayer serverPlayer) DepotSavedData.get(serverPlayer).fillStackedContents(contents);
    }

    @Override
    public void clearCraftingContent() {
        craftSlots.clearContent();
        resultSlots.clearContent();
    }

    @Override
    public boolean recipeMatches(RecipeHolder<CraftingRecipe> holder) {
        return holder.value().matches(craftSlots.asCraftInput(), player.level());
    }

    @Override
    public int getResultSlotIndex() {
        return 0;
    }

    @Override
    public int getGridWidth() {
        return 3;
    }

    @Override
    public int getGridHeight() {
        return 3;
    }

    @Override
    public int getSize() {
        return craftingUnlocked ? 10 : 0;
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    public boolean shouldMoveToInventory(int slot) {
        return slot != 0;
    }
}
