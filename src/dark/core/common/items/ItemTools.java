package dark.core.common.items;

import java.util.List;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatMessageComponent;
import net.minecraft.util.Icon;
import net.minecraft.world.World;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import universalelectricity.core.block.IConductor;
import universalelectricity.core.block.IElectrical;
import universalelectricity.core.block.IElectricalStorage;
import universalelectricity.core.electricity.ElectricityDisplay;
import universalelectricity.core.electricity.ElectricityDisplay.ElectricUnit;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dark.api.IToolReadOut;
import dark.api.IToolReadOut.EnumTools;
import dark.core.common.DarkMain;
import dark.core.prefab.helpers.FluidHelper;
import dark.core.prefab.items.ItemBasic;

public class ItemTools extends ItemBasic
{
    Icon pipeGuage, multiMeter;

    public ItemTools(int id, Configuration config)
    {
        super(id, "DMTools", config);
        this.setHasSubtypes(true);
        this.setCreativeTab(CreativeTabs.tabTools);
        this.setMaxStackSize(1);

    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerIcons(IconRegister iconRegister)
    {
        this.pipeGuage = iconRegister.registerIcon(DarkMain.getInstance().PREFIX + "PipeGauge");
        this.multiMeter = iconRegister.registerIcon(DarkMain.getInstance().PREFIX + "multi-Meter");
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Icon getIconFromDamage(int meta)
    {
        if (meta == 0)
        {
            return pipeGuage;
        }
        if (meta == 1)
        {
            return multiMeter;
        }
        return null;
    }

    @Override
    public String getUnlocalizedName(ItemStack itemStack)
    {
        int meta = itemStack.getItemDamage();
        if (meta == 0)
        {
            return "item." + "PipeGauge";
        }
        else if (meta == 1)
        {
            return "item." + "MultiMeter";
        }
        return "item." + this.getUnlocalizedName() + "." + meta;
    }

    @Override
    public void getSubItems(int par1, CreativeTabs par2CreativeTabs, List par3List)
    {
        par3List.add(new ItemStack(this, 1, 0));
        par3List.add(new ItemStack(this, 1, 1));
    }

    @Override
    public boolean onItemUse(ItemStack itemStack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ)
    {
        if (!world.isRemote)
        {
            TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
            EnumTools tool = EnumTools.get(itemStack.getItemDamage());

            if (tool != null)
            {
                if (tileEntity instanceof IToolReadOut)
                {
                    String output = ((IToolReadOut) tileEntity).getMeterReading(player, ForgeDirection.getOrientation(side), tool);
                    if (output != null && !output.isEmpty())
                    {
                        if (output.length() > 100)
                        {
                            output = output.substring(0, 100);
                        }
                        output.trim();
                        player.sendChatToPlayer(ChatMessageComponent.func_111066_d("ReadOut> " + output));
                        return true;
                    }
                }
                if (tool == EnumTools.PIPE_GUAGE)
                {
                    if (tileEntity instanceof IFluidHandler)
                    {
                        FluidTankInfo[] tanks = ((IFluidHandler) tileEntity).getTankInfo(ForgeDirection.getOrientation(side));
                        if (tanks != null)
                        {
                            player.sendChatToPlayer(ChatMessageComponent.func_111066_d("FluidHandler> Side:" + ForgeDirection.getOrientation(side).toString() + " Tanks:" + tanks.length));
                            for (FluidStack stack : FluidHelper.getFluidList(tanks))
                            {
                                player.sendChatToPlayer(ChatMessageComponent.func_111066_d("Fluid>" + stack.amount + "mb of " + stack.getFluid().getName()));
                            }
                            return true;
                        }
                    }
                }
                //TODO add shift click support to bring up a easier to read GUI or link to the block and add an on screen gui so the player can toy with a design and be updated
                if (tool == EnumTools.MULTI_METER)
                {
                    //TODO filter all units threw UE unit helper to created nicer looking output text
                    player.sendChatToPlayer(ChatMessageComponent.func_111066_d("Side>" + ForgeDirection.getOrientation(side).toString()));
                    boolean out = false;
                    // Output electrical info
                    if (tileEntity instanceof IElectrical)
                    {
                        player.sendChatToPlayer(ChatMessageComponent.func_111066_d("   Voltage> " + ElectricityDisplay.getDisplay(((IElectrical) tileEntity).getVoltage(), ElectricUnit.VOLTAGE, 2, true)));
                        if (((IElectrical) tileEntity).getRequest(ForgeDirection.getOrientation(side).getOpposite()) > 0)
                        {
                            player.sendChatToPlayer(ChatMessageComponent.func_111066_d("   RequiredWatts> " + ElectricityDisplay.getDisplay(((IElectrical) tileEntity).getRequest(ForgeDirection.getOrientation(side).getOpposite()), ElectricUnit.WATT, 2, true)));
                        }
                        if (((IElectrical) tileEntity).getProvide(ForgeDirection.getOrientation(side).getOpposite()) > 0)
                        {
                            player.sendChatToPlayer(ChatMessageComponent.func_111066_d("   AvailableWatts> " + ElectricityDisplay.getDisplay(((IElectrical) tileEntity).getProvide(ForgeDirection.getOrientation(side).getOpposite()), ElectricUnit.WATT, 2, true)));
                        }
                        out = true;
                    }
                    //Output battery info
                    if (tileEntity instanceof IElectricalStorage)
                    {
                        player.sendChatToPlayer(ChatMessageComponent.func_111066_d("   EnergyStored> " + ElectricityDisplay.getDisplay(((IElectricalStorage) tileEntity).getEnergyStored(), ElectricUnit.WATT, 2, true) + " out of " + ElectricityDisplay.getDisplay(((IElectricalStorage) tileEntity).getMaxEnergyStored(), ElectricUnit.WATT, 2, true) + " Max"));
                        out = true;
                    }
                    //Output wire info
                    if (tileEntity instanceof IConductor)
                    {
                        out = true;
                        player.sendChatToPlayer(ChatMessageComponent.func_111066_d("   Resistance> " + ElectricityDisplay.getDisplay(((IConductor) tileEntity).getResistance(), ElectricUnit.RESISTANCE, 2, true) + " | " + "AmpMax> " + ElectricityDisplay.getDisplay(((IConductor) tileEntity).getCurrentCapacity(), ElectricUnit.AMPERE, 2, true)));

                        if (((IConductor) tileEntity).getNetwork() != null)
                        {
                            player.sendChatToPlayer(ChatMessageComponent.func_111066_d("   Network>" + ((IConductor) tileEntity).getNetwork().toString()));

                            player.sendChatToPlayer(ChatMessageComponent.func_111066_d(String.format("   Network>WattRequired> " + (((IConductor) tileEntity).getNetwork().getRequest() != null ? ElectricityDisplay.getDisplay(((IConductor) tileEntity).getNetwork().getRequest().getWatts(), ElectricUnit.WATT, 2, true) : "Error") + " | " + "TotalResistance> " + ElectricityDisplay.getDisplay(((IConductor) tileEntity).getNetwork().getTotalResistance(), ElectricUnit.RESISTANCE, 2, true))));
                        }
                    }
                    //If no ouput was created suggest that the block is not valid for connection
                    if (!out)
                    {
                        player.sendChatToPlayer(ChatMessageComponent.func_111066_d("   Error failed to connect to block"));

                    }
                }
            }

        }

        return false;
    }

}
