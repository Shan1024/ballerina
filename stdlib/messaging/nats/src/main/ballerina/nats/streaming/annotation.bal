// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

#  NATS Streaming Subscription Parameters.
#
# + subject - Name of the subject to which it is subscribed 
# + queueName - The name of the queue group to which the subscription belongs 
# + durableName - If set, this will survive client restarts
# + maxInFlight - The number of messages the cluster will have inflight without an ACK
# + ackWait - The time the cluster will wait for an ACK for a given message (in Seconds)
# + subscriptionTimeout - The time (in Seconds) the subscription will wait if a network failure occurs during the creation of it.
# + manualAck - Do manual ACKs
# + startPosition - The position to start receiving messages 
public type StreamingSubscriptionConfigData record {|
   string subject;
   string queueName?;
   string durableName?;
   int maxInFlight = 1024;
   int ackWait = 30;
   int subscriptionTimeout = 2;
   boolean manualAck = false;
   StartPosition startPosition = NEW_ONLY;
|};

# Streaming subscription configuration annotation.
public annotation StreamingSubscriptionConfigData StreamingSubscriptionConfig on service;

# The position to start receiving messages.
# NEW_ONLY - Specifies that message delivery should start with the messages, which are published after the subscription
# is created.
# LAST_RECEIVED - Specifies that message delivery should start with the last (most recent) message stored for
# this subject.
# TimeDeltaStart record - Specifies that message delivery should start with a given historical time delta (from now).
# StartSequence - Specifies that message delivery should start at the given sequence number.
# FIRST - Specifies that message delivery should begin at the oldest available message for this subject.
public type StartPosition NEW_ONLY | LAST_RECEIVED | TimeDeltaStart | StartSequence | FIRST;

public const NEW_ONLY = "NEW_ONLY";
public const LAST_RECEIVED = "LAST_RECEIVED";
public const FIRST = "FIRST";

# Represents the starting position based on a certain point of time, which lies a duration of `timeDelta` back from the
# current moment. This is measured in the time unit specified as the `timeUnit`.
#
# + timeDelta - The duration of time
# + timeUnit - The unit of time corresponding to the `timeDelta`
public type TimeDeltaStart record {|
    int timeDelta;
    TimeUnit timeUnit = SECONDS;
|};

# Specifies the sequence number from which receiving messages starts.
public type StartSequence int;

# Types of time units.
public type TimeUnit NANOSECONDS | MICROSECONDS | MILLISECONDS | SECONDS | MINUTES | HOURS | DAYS;

public const DAYS = "DAYS";
public const HOURS = "HOURS";
public const MINUTES = "MINUTES";
public const SECONDS = "SECONDS";
public const MILLISECONDS = "MILLISECONDS";
public const MICROSECONDS = "MICROSECONDS";
public const NANOSECONDS = "NANOSECONDS";
