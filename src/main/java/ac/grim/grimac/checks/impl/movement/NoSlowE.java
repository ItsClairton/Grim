package ac.grim.grimac.checks.impl.movement;

import static com.github.retrooper.packetevents.protocol.potion.PotionTypes.BLINDNESS;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

@CheckData(name = "NoSlowE", setback = 5, experimental = true)
public class NoSlowE extends Check implements PostPredictionCheck {
    public NoSlowE(GrimPlayer player) {
        super(player);
    }

    public boolean startedSprintingBeforeBlind = false;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!startedSprintingBeforeBlind) {
            return;
        }

        if (event.getPacketType() != PacketType.Play.Client.ENTITY_ACTION) {
            return;
        }

        final var packet = lastWrapper(event,
                WrapperPlayClientEntityAction.class,
                () -> new WrapperPlayClientEntityAction(event));

        if (packet.getAction() != WrapperPlayClientEntityAction.Action.START_SPRINTING) {
            return;
        }

        startedSprintingBeforeBlind = false;
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked()) {
            return;
        }

        if (player.compensatedEntities.getSelf().hasPotionEffect(BLINDNESS)) {
            if (player.isSprinting && !startedSprintingBeforeBlind) {
                if (flagWithSetback()) alert();
            } else {
                reward();
            }
        }
    }

}
