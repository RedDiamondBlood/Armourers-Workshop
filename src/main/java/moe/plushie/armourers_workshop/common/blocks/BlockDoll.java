package moe.plushie.armourers_workshop.common.blocks;

import java.util.Random;

import com.mojang.authlib.GameProfile;

import moe.plushie.armourers_workshop.client.texture.PlayerTexture;
import moe.plushie.armourers_workshop.common.Contributors;
import moe.plushie.armourers_workshop.common.Contributors.Contributor;
import moe.plushie.armourers_workshop.common.config.ConfigHandler;
import moe.plushie.armourers_workshop.common.holiday.ModHolidays;
import moe.plushie.armourers_workshop.common.lib.EnumGuiId;
import moe.plushie.armourers_workshop.common.lib.LibBlockNames;
import moe.plushie.armourers_workshop.common.tileentities.TileEntityMannequin;
import moe.plushie.armourers_workshop.utils.BlockUtils;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockDoll extends AbstractModBlockContainer {

    private static final String TAG_OWNER = "owner";
    private static final String TAG_IMAGE_URL = "imageUrl";
    
    private final boolean isValentins;
    private static final AxisAlignedBB AABB = new AxisAlignedBB(0.2F, 0F, 0.2F, 0.8F, 0.95F, 0.8F);
    
    public BlockDoll() {
        super(LibBlockNames.DOLL, Material.CIRCUITS, SoundType.METAL, !ConfigHandler.hideDollFromCreativeTabs);
        setLightOpacity(0);
        //setBlockBounds(0.2F, 0F, 0.2F, 0.8F, 0.95F, 0.8F);
        isValentins = ModHolidays.VALENTINES.isHolidayActive();
        setSortPriority(198);
    }
    
    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB;
    }
    
    public static ItemStack getStackWithTexture(PlayerTexture playerTexture) {
        ItemStack result = new ItemStack(ModBlocks.doll);
        result.setTagCompound(new NBTTagCompound());
        playerTexture.writeToNBT(result.getTagCompound());
        return result;
    }
    
    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote) {
            BlockUtils.dropInventoryBlocks(worldIn, pos);
        }
        super.breakBlock(worldIn, pos, state);
    }
    
    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te != null && te instanceof TileEntityMannequin) {
            int l = MathHelper.floor((double) (placer.rotationYaw * 16.0F / 360.0F) + 0.5D) & 15;
            ((TileEntityMannequin) te).PROP_ROTATION.set(l);
            if (!worldIn.isRemote) {
                if (stack.hasTagCompound()) {
                    NBTTagCompound compound = stack.getTagCompound();
                    GameProfile gameProfile = null;
                    if (compound.hasKey(TAG_OWNER, 10)) {
                        gameProfile = NBTUtil.readGameProfileFromNBT(compound.getCompoundTag(TAG_OWNER));
                        ((TileEntityMannequin) te).PROP_OWNER.set(gameProfile);
                    }
                    if (compound.hasKey(TAG_IMAGE_URL, Constants.NBT.TAG_STRING)) {
                        ((TileEntityMannequin) te).PROP_IMAGE_URL.set(compound.getString(TAG_IMAGE_URL));
                    }
                }
            }
        }
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        ParticleManager particleManager = Minecraft.getMinecraft().effectRenderer;
        if (isValentins) {
            if (rand.nextFloat() * 100 > 75) {
                Particle particle = particleManager.spawnEffectParticle(EnumParticleTypes.HEART.getParticleID(), pos.getX() - 0.2F + rand.nextFloat() * 0.6F, pos.getY(), pos.getZ() - 0.2F + rand.nextFloat() * 0.6F, 0, 0, 0, null);
                Minecraft.getMinecraft().effectRenderer.addEffect(particle);
            }
        }
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity != null && tileEntity instanceof TileEntityMannequin) {
            TileEntityMannequin te = (TileEntityMannequin) tileEntity;
            if (te != null && te.PROP_RENDER_EXTRAS.get()) {
                Contributor contributor = Contributors.INSTANCE.getContributor(te.PROP_OWNER.get());
                if (contributor != null & te.PROP_VISIBLE.get()) {
                    for (int i = 0; i < 6; i++) {
                        Particle particle = particleManager.spawnEffectParticle(EnumParticleTypes.SPELL.getParticleID(), pos.getX() + rand.nextFloat() * 1F, pos.getY(), pos.getZ() + rand.nextFloat() * 1F, 0, 0, 0, null);
                        particle.setRBGColorF((float) (contributor.r & 0xFF) / 255F, (float) (contributor.g & 0xFF) / 255F, (float) (contributor.b & 0xFF) / 255F);
                        Minecraft.getMinecraft().effectRenderer.addEffect(particle);
                    }
                }
            }
        }
    }
    
    @Override
    public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis) {
        if (world.isRemote) {
            return false;
        }
        TileEntity te = world.getTileEntity(pos);
        if (te != null  && te instanceof TileEntityMannequin) {
            int rotation = ((TileEntityMannequin)te).PROP_ROTATION.get();
            rotation++;
            if (rotation > 15) {
                rotation = 0;
            }
            ((TileEntityMannequin)te).PROP_ROTATION.set(rotation);
        }
        return true;
    }
    
    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        ItemStack stack = new ItemStack(ModBlocks.doll, 1);
        int meta = 0;// world.getBlockMetadata(x, y, z);
        int yOffset = 0;
        if (meta == 1) {
            yOffset = -1;
        }
        TileEntity te = world.getTileEntity(pos);;
        if (te != null && te instanceof TileEntityMannequin) {
            TileEntityMannequin teMan = (TileEntityMannequin) te;
            if (teMan.PROP_OWNER.get() != null) {
                NBTTagCompound profileTag = new NBTTagCompound();
                NBTUtil.writeGameProfile(profileTag, teMan.PROP_OWNER.get());
                stack.setTagCompound(new NBTTagCompound());
                stack.getTagCompound().setTag(TAG_OWNER, profileTag);
            }
        }
        return stack;
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public boolean addDestroyEffects(World world, BlockPos pos, ParticleManager manager) {
        return true;
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public boolean addHitEffects(IBlockState state, World worldObj, RayTraceResult target, ParticleManager manager) {
        return true;
    }
    
    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!playerIn.canPlayerEdit(pos, facing, playerIn.getHeldItem(hand))) {
            return false;
        }
        openGui(playerIn, EnumGuiId.MANNEQUIN, worldIn, pos, state, facing);
        return true;
    }
    
    @Override
    public int quantityDropped(IBlockState state, int fortune, Random random) {
        return 0;
    }
    
    @Override
    public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_) {
        return new TileEntityMannequin(true);
    }
    
    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }
    
    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }
    
    @Override
    public boolean isFullBlock(IBlockState state) {
        return false;
    }
}
