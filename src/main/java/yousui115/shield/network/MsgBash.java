package yousui115.shield.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MsgBash implements IMessage
{
    private int targetID;
    private int power;
    private int amount;

    /**
     * ■コンストラクタ(必須！)
     */
    public MsgBash(){}

    /**
     * ■コンストラクタ
     */
    public MsgBash(Entity entityIn, int powerIn, int amountIn)
    {
        this.targetID = entityIn.getEntityId();
        this.power = powerIn;
        this.amount = amountIn;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.targetID = buf.readInt();
        this.power = buf.readInt();
        this.amount = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(this.targetID);
        buf.writeInt(this.power);
        buf.writeInt(this.amount);
    }

    public int getTargetID() { return this.targetID; }
    public int getPower() { return this.power; }
    public int getAmount() { return this.amount; }

}
