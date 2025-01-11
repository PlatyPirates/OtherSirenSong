// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.networktables.DoubleTopic;
import edu.wpi.first.networktables.IntegerTopic;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.util.sendable.SendableBuilder;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
//import com.kauailabs.navx.frc.AHRS;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.RunCommand;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.commands.*;
import frc.robot.subsystems.*;

/**
 * This class is where the bulk of the robot should be declared. Since Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in the {@link Robot}
 * periodic methods (other than the scheduler calls). Instead, the structure of the robot (including
 * subsystems, commands, and trigger mappings) should be declared here.
 */
public class RobotContainer {
  // The robot's subsystems and commands are defined here...
  public final Drive_Train _drive_Train = new Drive_Train();
  //private final Joystick _driver = new Joystick(0);
  //private final Joystick _operator = new Joystick(1);
  public final CommandXboxController _driver = new CommandXboxController(0);
  private final CommandXboxController _operator = new CommandXboxController(1);

  public final LeftClaw _leftClaw = new LeftClaw();
  public final RightClaw _rightClaw = new RightClaw();
  private final Intake_Bar _intakeBar = new Intake_Bar();
  private final Intake_Belt _intakeBelt = new Intake_Belt(){};
  private final Trap_Rollers _trapRollers = new Trap_Rollers();
  public final Limit_Switch _limitSwitch = new Limit_Switch(9);

  public final LeftBalancer _leftBalancer = new LeftBalancer();
  public final RightBalancer _rightBalancer = new RightBalancer();

  private static int driveConstant = 1;
  private DoubleTopic centerTagX;
  private IntegerTopic centerImageX;
  private SendableChooser<Command> autoChooser = new SendableChooser<Command>();


  public NetworkTableInstance netInst;

  /** The container for the robot. Contains subsystems, OI devices, and commands. */
  public RobotContainer() {
    // Configure the trigger bindings
    configureBindings();
    netInst = NetworkTableInstance.getDefault();

    _drive_Train.setDefaultCommand(
      Commands.run(
        () ->
        _drive_Train.drive(
                -_driver.getLeftY(), _driver.getRightX()*0.5),
                _drive_Train));
    
    new LimitSwitch(_limitSwitch).schedule();

      autoChooser.setDefaultOption("Cross Auto Line Only", new AMoveEnd(_drive_Train));
      autoChooser.addOption("Follow AprilTag (do nothing)", new AutonomousDrive(_drive_Train,netInst));
      autoChooser.addOption("Score in Amp", new AScoreInAmp(_drive_Train, _intakeBar, _intakeBelt, _trapRollers, netInst));
      autoChooser.addOption("Align With Trap (RIGHT)", new AAlignTrapLeft(_drive_Train, _intakeBelt, _intakeBar, _trapRollers, _rightClaw, netInst));
      SmartDashboard.putData("Auto Choices", autoChooser);

      SmartDashboard.putString("Team Station", DriverStation.getAlliance().toString() + " (" + DriverStation.getLocation() + ")");
  }

  /**
   * Use this method to define your trigger->command mappings. Triggers can be created via the
   * {@link Trigger#Trigger(java.util.function.BooleanSupplier)} constructor with an arbitrary
   * predicate, or via the named factories in {@link
   * edu.wpi.first.wpilibj2.command.button.CommandGenericHID}'s subclasses for {@link
   * CommandXboxController Xbox}/{@link edu.wpi.first.wpilibj2.command.button.CommandPS4Controller
   * PS4} controllers or {@link edu.wpi.first.wpilibj2.command.button.CommandJoystick Flight
   * joysticks}.
   */


  private void configureBindings() {
    _operator
      .leftBumper()
      .whileTrue(new LeftClawUp(_leftClaw));
      //.whileTrue(Commands.runOnce(() -> _leftClaw.ClawUp(), _leftClaw));
    
    _operator
      .rightBumper()
      .whileTrue(new RightClawUp(_rightClaw));
      //.whileTrue(Commands.runOnce(() -> _rightClaw.ClawUp(), _rightClaw));
    
    _operator
      .leftTrigger()
      .whileTrue(new LeftClawDown(_leftClaw));
      //.whileTrue(Commands.runOnce(() -> _leftClaw.ClawDown(), _leftClaw));
    
    _operator
      .rightTrigger()
      .whileTrue(new RunCommand(_rightClaw::ClawDown, _rightClaw));
      //.whileTrue(Commands.runOnce(() -> _rightClaw.ClawDown(), _rightClaw));
    //intake
      _operator 
      .b()
      .whileTrue(new RunCommand(_intakeBar::IntakeBarIn, _intakeBar))
      .whileTrue(new RunCommand(_intakeBelt::IntakeBeltIn, _intakeBelt));
    //outtake amp 
      _operator 
      .x()
      .whileTrue(new RunCommand(_intakeBar::IntakeBarIn, _intakeBar))
      .whileTrue(new RunCommand(_intakeBelt::IntakeBeltOut, _intakeBelt));
    //y for getting it up there 
      _driver 
      .y()
      .whileTrue(new RunCommand(_intakeBelt::TrapBelt, _intakeBelt))
      .whileTrue(new RunCommand(_intakeBar::TrapBar, _intakeBar))
      .whileTrue(new RunCommand(_trapRollers::TrapScoreRollers, _trapRollers));
      //A for shooting (digital input limit switch)
      _driver 
      .a()
      .whileTrue(new RunCommand(_trapRollers::TrapRollersIn, _trapRollers));
      
      _driver 
      .start()
      .whileTrue(new RunCommand(_drive_Train::encoderReset, _drive_Train));
      
      _operator
      .start()
      .whileTrue(new RunCommand(_leftClaw::resetEncoder, _leftClaw))
      .whileTrue(new RunCommand(_rightClaw::resetEncoder,_rightClaw));

      _driver 
      .x()
      .whileTrue(new RunCommand(_trapRollers::TrapRollersOut, _trapRollers));
      _driver
      .leftTrigger()
      .whileTrue(
        Commands.run(
        () ->
        _drive_Train.driveReverse(
                -_driver.getLeftY(), _driver.getRightX()*0.5),
                _drive_Train));

      _operator
      .y()
      .whileTrue(new RunCommand(_leftBalancer::BalancerDown, _leftBalancer))
      .whileTrue(new RunCommand(_rightBalancer::BalancerDown, _rightBalancer));

      _operator
      .a()
      .whileTrue(new RunCommand(_leftBalancer::BalancerUp, _leftBalancer))
      .whileTrue(new RunCommand(_rightBalancer::BalancerUp, _rightBalancer));

  }
  /**
   * Use this to pass the autonomous command to the main {@link Robot} class.
   *
   * @return the command to run in autonomous
  */ 
  public static void switchControls(){
    driveConstant *= -1;
    SmartDashboard.putNumber("DriveConstant", driveConstant);
  }

  public Command getAutonomousCommand() {
    // An example command will be run in autonomous
    return autoChooser.getSelected();
  }
}
